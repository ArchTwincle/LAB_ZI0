package com.example.tourism_service.service.binary;

import com.example.tourism_service.entity.MalwareSignature;
import com.example.tourism_service.entity.SignatureStatus;
import com.example.tourism_service.repository.MalwareSignatureRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class BinarySignatureExportService {

    private final MalwareSignatureRepository malwareSignatureRepository;
    private final BinarySignatureDataWriter dataWriter;
    private final BinarySignatureManifestWriter manifestWriter;

    public BinarySignatureExportService(MalwareSignatureRepository malwareSignatureRepository,
                                        BinarySignatureDataWriter dataWriter,
                                        BinarySignatureManifestWriter manifestWriter) {
        this.malwareSignatureRepository = malwareSignatureRepository;
        this.dataWriter = dataWriter;
        this.manifestWriter = manifestWriter;
    }

    public BinarySignaturePackage buildFullPackage() {
        List<MalwareSignature> signatures = malwareSignatureRepository
                .findAllByStatusOrderByUpdatedAtDesc(SignatureStatus.ACTUAL);
        return buildPackage(BinaryExportType.FULL, null, signatures);
    }

    public BinarySignaturePackage buildIncrementPackage(Instant since) {
        if (since == null) {
            throw new IllegalArgumentException("Параметр since обязателен");
        }

        List<MalwareSignature> signatures = malwareSignatureRepository
                .findAllByUpdatedAtAfterOrderByUpdatedAtDesc(since);
        return buildPackage(BinaryExportType.INCREMENT, since, signatures);
    }

    public BinarySignaturePackage buildByIdsPackage(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return buildPackage(BinaryExportType.BY_IDS, null, Collections.emptyList());
        }

        List<MalwareSignature> signatures = malwareSignatureRepository.findAllByIdIn(ids);
        return buildPackage(BinaryExportType.BY_IDS, null, signatures);
    }

    private BinarySignaturePackage buildPackage(BinaryExportType exportType,
                                                Instant since,
                                                List<MalwareSignature> signatures) {
        BinarySignatureDataWriter.BinaryDataWriteResult dataResult = dataWriter.write(signatures);
        byte[] manifestBytes = manifestWriter.write(
                exportType,
                since,
                dataResult.dataBytes(),
                dataResult.entries()
        );

        return new BinarySignaturePackage(manifestBytes, dataResult.dataBytes());
    }
}
