package com.example.tourism_service.service;

import com.example.tourism_service.dto.*;
import com.example.tourism_service.entity.*;
import com.example.tourism_service.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final LicenseHistoryRepository licenseHistoryRepository;
    private final ProductRepository productRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final UserRepository userRepository;
    private final SignatureService signatureService;

    public LicenseService(LicenseRepository licenseRepository,
                          DeviceRepository deviceRepository,
                          DeviceLicenseRepository deviceLicenseRepository,
                          LicenseHistoryRepository licenseHistoryRepository,
                          ProductRepository productRepository,
                          LicenseTypeRepository licenseTypeRepository,
                          UserRepository userRepository,
                          SignatureService signatureService) {
        this.licenseRepository = licenseRepository;
        this.deviceRepository = deviceRepository;
        this.deviceLicenseRepository = deviceLicenseRepository;
        this.licenseHistoryRepository = licenseHistoryRepository;
        this.productRepository = productRepository;
        this.licenseTypeRepository = licenseTypeRepository;
        this.userRepository = userRepository;
        this.signatureService = signatureService;
    }

    @Transactional
    public License createLicense(CreateLicenseRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Владелец лицензии не найден"));

        Product product = productRepository.findById(UUID.fromString(request.getProductId()))
                .orElseThrow(() -> new RuntimeException("Продукт не найден"));

        LicenseType licenseType = licenseTypeRepository.findById(UUID.fromString(request.getLicenseTypeId()))
                .orElseThrow(() -> new RuntimeException("Тип лицензии не найден"));

        License license = new License();
        license.setCode(generateLicenseCode());
        license.setUser(user);
        license.setOwner(owner);
        license.setProduct(product);
        license.setType(licenseType);

        // Лицензия создаётся, но ещё не активирована
        license.setFirstActivationDate(null);
        license.setEndingDate(null);

        license.setBlocked(false);
        license.setDeviceCount(request.getDeviceCount() != null ? request.getDeviceCount() : 1);
        license.setDescription(request.getDescription());

        License savedLicense = licenseRepository.save(license);

        LicenseHistory history = new LicenseHistory();
        history.setLicense(savedLicense);
        history.setUser(owner);
        history.setStatus("CREATED");
        history.setChangeDate(LocalDate.now());
        history.setDescription("Лицензия создана");
        licenseHistoryRepository.save(history);

        return savedLicense;
    }

    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request) {
        License license = licenseRepository.findByCode(request.getLicenseCode())
                .orElseThrow(() -> new RuntimeException("Лицензия не найдена"));

        if (Boolean.TRUE.equals(license.getBlocked())) {
            throw new RuntimeException("Лицензия заблокирована");
        }

        if (license.getEndingDate() != null &&
                license.getEndingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Срок действия лицензии истек");
        }

        if (license.getProduct() != null &&
                Boolean.TRUE.equals(license.getProduct().getIsBlocked())) {
            throw new RuntimeException("Продукт заблокирован");
        }

        Device device = deviceRepository.findByMacAddress(request.getMacAddress())
                .orElseGet(() -> {
                    Device newDevice = new Device();
                    newDevice.setMacAddress(request.getMacAddress());
                    newDevice.setName(request.getDeviceName());
                    newDevice.setUser(license.getUser());
                    return deviceRepository.save(newDevice);
                });

        boolean alreadyActivated = deviceLicenseRepository
                .findByLicenseIdAndDeviceId(license.getId(), device.getId())
                .isPresent();

        if (!alreadyActivated) {
            long currentDeviceCount = deviceLicenseRepository.countByLicenseId(license.getId());

            if (currentDeviceCount >= license.getDeviceCount()) {
                throw new RuntimeException("Превышен лимит устройств для лицензии");
            }

            DeviceLicense deviceLicense = new DeviceLicense();
            deviceLicense.setLicense(license);
            deviceLicense.setDevice(device);
            deviceLicense.setActivationDate(LocalDate.now());
            deviceLicenseRepository.save(deviceLicense);
        }

        if (license.getFirstActivationDate() == null) {
            license.setFirstActivationDate(LocalDate.now());

            if (license.getType() != null) {
                license.setEndingDate(
                        LocalDate.now().plusDays(
                                license.getType().getDefaultDurationInDays()
                        )
                );
            }

            licenseRepository.save(license);
        }

        if (!alreadyActivated) {
            LicenseHistory history = new LicenseHistory();
            history.setLicense(license);
            history.setUser(license.getOwner());
            history.setStatus("ACTIVATED");
            history.setChangeDate(LocalDate.now());
            history.setDescription("Лицензия активирована на устройстве: " + request.getMacAddress());
            licenseHistoryRepository.save(history);
        }

        return buildTicketResponse(license, device);
    }

    @Transactional(readOnly = true)
    public TicketResponse checkLicense(CheckLicenseRequest request) {
        License license = licenseRepository.findByCode(request.getLicenseCode())
                .orElseThrow(() -> new RuntimeException("Лицензия не найдена"));

        if (Boolean.TRUE.equals(license.getBlocked())) {
            throw new RuntimeException("Лицензия заблокирована");
        }

        if (license.getEndingDate() != null &&
                license.getEndingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Срок действия лицензии истек");
        }

        if (license.getProduct() != null &&
                Boolean.TRUE.equals(license.getProduct().getIsBlocked())) {
            throw new RuntimeException("Продукт заблокирован");
        }

        Device device = deviceRepository.findByMacAddress(request.getMacAddress())
                .orElseThrow(() -> new RuntimeException("Устройство не найдено"));

        deviceLicenseRepository.findByLicenseIdAndDeviceId(license.getId(), device.getId())
                .orElseThrow(() -> new RuntimeException("Лицензия не активирована на этом устройстве"));

        return buildTicketResponse(license, device);
    }

    @Transactional
    public License renewLicense(RenewLicenseRequest request) {
        License license = licenseRepository.findByCode(request.getLicenseCode())
                .orElseThrow(() -> new RuntimeException("Лицензия не найдена"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (license.getFirstActivationDate() == null) {
            throw new RuntimeException("Нельзя продлить неактивированную лицензию");
        }

        int extraDays = request.getExtraDays() != null ? request.getExtraDays() : 0;
        LocalDate baseDate = license.getEndingDate() != null && license.getEndingDate().isAfter(LocalDate.now())
                ? license.getEndingDate()
                : LocalDate.now();

        license.setEndingDate(baseDate.plusDays(extraDays));
        License saved = licenseRepository.save(license);

        LicenseHistory history = new LicenseHistory();
        history.setLicense(saved);
        history.setUser(user);
        history.setStatus("RENEWED");
        history.setChangeDate(LocalDate.now());
        history.setDescription(
                request.getDescription() != null ? request.getDescription() : "Лицензия продлена"
        );
        licenseHistoryRepository.save(history);

        return saved;
    }

    private TicketResponse buildTicketResponse(License license, Device device) {
        LocalDateTime now = LocalDateTime.now();

        long ttlSeconds = 0;
        if (license.getEndingDate() != null) {
            LocalDateTime expirationDateTime = license.getEndingDate().plusDays(1).atStartOfDay();
            ttlSeconds = Math.max(0, ChronoUnit.SECONDS.between(now, expirationDateTime));
        }

        Ticket ticket = new Ticket();
        ticket.setServerDate(now);
        ticket.setTicketLifetimeSeconds(ttlSeconds);
        ticket.setActivationDate(license.getFirstActivationDate());
        ticket.setExpirationDate(license.getEndingDate());
        ticket.setUserId(license.getUser().getId());
        ticket.setDeviceId(device.getId().toString());
        ticket.setBlocked(license.getBlocked());

        String signature = signatureService.signTicket(ticket);
        return new TicketResponse(ticket, signature);
    }

    private String generateLicenseCode() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}