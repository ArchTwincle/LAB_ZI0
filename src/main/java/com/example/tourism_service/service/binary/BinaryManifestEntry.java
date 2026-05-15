package com.example.tourism_service.service.binary;

import com.example.tourism_service.entity.MalwareSignature;

final class BinaryManifestEntry {

    private final MalwareSignature signature;
    private final long dataOffset;
    private final long dataLength;

    BinaryManifestEntry(MalwareSignature signature, long dataOffset, long dataLength) {
        this.signature = signature;
        this.dataOffset = dataOffset;
        this.dataLength = dataLength;
    }

    MalwareSignature getSignature() {
        return signature;
    }

    long getDataOffset() {
        return dataOffset;
    }

    long getDataLength() {
        return dataLength;
    }
}
