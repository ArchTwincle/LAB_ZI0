package com.example.tourism_service.service.binary;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public class MultipartMixedResponseFactory {

    public HttpEntity<MultiValueMap<String, Object>> create(BinarySignaturePackage binaryPackage) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("manifest", createBinaryPart("manifest.bin", binaryPackage.manifestBytes()));
        body.add("data", createBinaryPart("data.bin", binaryPackage.dataBytes()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("multipart/mixed"));

        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<ByteArrayResource> createBinaryPart(String filename, byte[] bytes) {
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(bytes.length);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return new HttpEntity<>(resource, headers);
    }
}
