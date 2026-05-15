package com.example.tourism_service.service.binary;

import com.example.tourism_service.entity.MalwareSignature;
import com.example.tourism_service.entity.SignatureStatus;
import com.example.tourism_service.service.MalwareDigitalSignatureService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Component
public class BinarySignatureManifestWriter {

    private static final String MAGIC = "MF-Staviskiy";
    private static final int VERSION = 1;

    private final MalwareDigitalSignatureService digitalSignatureService;

    public BinarySignatureManifestWriter(MalwareDigitalSignatureService digitalSignatureService) {
        this.digitalSignatureService = digitalSignatureService;
    }

    public byte[] write(BinaryExportType exportType,
                        Instant since,
                        byte[] dataBytes,
                        List<BinaryManifestEntry> entries) {
        byte[] unsignedManifest = writeUnsignedManifest(exportType, since, dataBytes, entries);
        byte[] manifestSignature = digitalSignatureService.signRaw(unsignedManifest);

        BinaryProtocolWriter writer = new BinaryProtocolWriter();
        writer.writeBytes(unsignedManifest);
        writer.writeU32(manifestSignature.length);
        writer.writeBytes(manifestSignature);
        return writer.toByteArray();
    }

    private byte[] writeUnsignedManifest(BinaryExportType exportType,
                                         Instant since,
                                         byte[] dataBytes,
                                         List<BinaryManifestEntry> entries) {
        BinaryProtocolWriter writer = new BinaryProtocolWriter();

        writer.writeBytes(MAGIC.getBytes(StandardCharsets.UTF_8));
        writer.writeU16(VERSION);
        writer.writeU8(exportType.getCode());
        writer.writeI64(Instant.now().toEpochMilli());
        writer.writeI64(exportType == BinaryExportType.INCREMENT && since != null ? since.toEpochMilli() : -1L);
        writer.writeU32(entries.size());
        writer.writeBytes(sha256(dataBytes));

        for (BinaryManifestEntry entry : entries) {
            writeEntry(writer, entry);
        }

        return writer.toByteArray();
    }

    private void writeEntry(BinaryProtocolWriter writer, BinaryManifestEntry entry) {
        MalwareSignature signature = entry.getSignature();
        byte[] recordSignatureBytes = Base64.getDecoder().decode(signature.getDigitalSignatureBase64());

        writer.writeUuid(signature.getId());
        writer.writeU8(statusCode(signature.getStatus()));
        writer.writeI64(signature.getUpdatedAt().toEpochMilli());
        writer.writeI64(entry.getDataOffset());
        writer.writeI64(entry.getDataLength());
        writer.writeU32(recordSignatureBytes.length);
        writer.writeBytes(recordSignatureBytes);
    }

    private int statusCode(SignatureStatus status) {
        if (status == SignatureStatus.ACTUAL) {
            return 1;
        }
        if (status == SignatureStatus.DELETED) {
            return 2;
        }
        throw new IllegalArgumentException("Неизвестный статус сигнатуры: " + status);
    }

    private byte[] sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось рассчитать SHA-256 для data.bin", e);
        }
    }
}
