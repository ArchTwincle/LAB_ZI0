package com.example.tourism_service.service.binary;

import com.example.tourism_service.entity.MalwareSignature;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class BinarySignatureDataWriter {

    private static final String MAGIC = "DB-Staviskiy";
    private static final int VERSION = 1;

    public BinaryDataWriteResult write(List<MalwareSignature> signatures) {
        List<BinaryManifestEntry> entries = new ArrayList<>();
        List<byte[]> recordBytes = new ArrayList<>();

        long currentOffset = 0;
        for (MalwareSignature signature : signatures) {
            byte[] oneRecord = writeRecord(signature);
            recordBytes.add(oneRecord);
            entries.add(new BinaryManifestEntry(signature, currentOffset, oneRecord.length));
            currentOffset += oneRecord.length;
        }

        BinaryProtocolWriter writer = new BinaryProtocolWriter();
        writer.writeBytes(MAGIC.getBytes(StandardCharsets.UTF_8));
        writer.writeU16(VERSION);
        writer.writeU32(signatures.size());

        for (byte[] record : recordBytes) {
            writer.writeBytes(record);
        }

        return new BinaryDataWriteResult(writer.toByteArray(), entries);
    }

    private byte[] writeRecord(MalwareSignature signature) {
        if (signature.getOffsetEnd() < signature.getOffsetStart()) {
            throw new IllegalArgumentException("offsetEnd не может быть меньше offsetStart для сигнатуры " + signature.getId());
        }

        BinaryProtocolWriter writer = new BinaryProtocolWriter();
        writer.writeUtf8String(signature.getThreatName());
        writer.writeByteArray(hexToBytes(signature.getFirstBytesHex()));
        writer.writeByteArray(hexToBytes(signature.getRemainderHashHex()));
        writer.writeI64(signature.getRemainderLength());
        writer.writeUtf8String(signature.getFileType());
        writer.writeI64(signature.getOffsetStart());
        writer.writeI64(signature.getOffsetEnd());
        return writer.toByteArray();
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isBlank()) {
            return new byte[0];
        }

        String normalized = hex.replaceAll("\\s+", "");
        if ((normalized.length() & 1) == 1) {
            throw new IllegalArgumentException("Hex-строка должна содержать четное количество символов");
        }

        byte[] result = new byte[normalized.length() / 2];
        for (int i = 0; i < normalized.length(); i += 2) {
            int high = Character.digit(normalized.charAt(i), 16);
            int low = Character.digit(normalized.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Некорректная hex-строка: " + hex);
            }
            result[i / 2] = (byte) ((high << 4) + low);
        }
        return result;
    }

    public record BinaryDataWriteResult(byte[] dataBytes, List<BinaryManifestEntry> entries) {
    }
}
