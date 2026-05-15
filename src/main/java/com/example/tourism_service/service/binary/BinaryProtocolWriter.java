package com.example.tourism_service.service.binary;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


final class BinaryProtocolWriter {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    void writeU8(int value) {
        out.write(value & 0xFF);
    }

    void writeU16(int value) {
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    void writeU32(long value) {
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    void writeI64(long value) {
        out.write((int) ((value >>> 56) & 0xFF));
        out.write((int) ((value >>> 48) & 0xFF));
        out.write((int) ((value >>> 40) & 0xFF));
        out.write((int) ((value >>> 32) & 0xFF));
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    void writeUuid(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("UUID не может быть null");
        }
        writeI64(value.getMostSignificantBits());
        writeI64(value.getLeastSignificantBits());
    }

    void writeUtf8String(String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        writeU32(bytes.length);
        writeBytes(bytes);
    }

    void writeByteArray(byte[] bytes) {
        byte[] safeBytes = bytes == null ? new byte[0] : bytes;
        writeU32(safeBytes.length);
        writeBytes(safeBytes);
    }

    void writeBytes(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            out.writeBytes(bytes);
        }
    }

    byte[] toByteArray() {
        return out.toByteArray();
    }
}
