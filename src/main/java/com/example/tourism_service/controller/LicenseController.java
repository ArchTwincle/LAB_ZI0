package com.example.tourism_service.controller;

import com.example.tourism_service.dto.*;
import com.example.tourism_service.entity.License;
import com.example.tourism_service.service.LicenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping("/create")
    public ResponseEntity<License> createLicense(@RequestBody CreateLicenseRequest request) {
        return ResponseEntity.ok(licenseService.createLicense(request));
    }

    @PostMapping("/activate")
    public ResponseEntity<TicketResponse> activateLicense(@RequestBody ActivateLicenseRequest request) {
        return ResponseEntity.ok(licenseService.activateLicense(request));
    }

    @PostMapping("/check")
    public ResponseEntity<TicketResponse> checkLicense(@RequestBody CheckLicenseRequest request) {
        return ResponseEntity.ok(licenseService.checkLicense(request));
    }

    @PostMapping("/renew")
    public ResponseEntity<License> renewLicense(@RequestBody RenewLicenseRequest request) {
        return ResponseEntity.ok(licenseService.renewLicense(request));
    }
}