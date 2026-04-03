package com.example.tourism_service.service;

import com.example.tourism_service.dto.Ticket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

@Service
public class SignatureService {

    @Value("${server.ssl.key-store}")
    private String keyStorePath;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.key-alias}")
    private String keyAlias;

    private final ObjectMapper objectMapper;

    public SignatureService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String signTicket(Ticket ticket) {
        try {
            String normalizedPath = keyStorePath.replace("classpath:", "");
            ClassPathResource resource = new ClassPathResource(normalizedPath);

            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (InputStream inputStream = resource.getInputStream()) {
                keyStore.load(inputStream, keyStorePassword.toCharArray());
            }

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(
                    keyAlias,
                    keyStorePassword.toCharArray()
            );

            String payload = objectMapper.writeValueAsString(ticket);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));

            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при формировании ЭЦП для Ticket", e);
        }
    }
}