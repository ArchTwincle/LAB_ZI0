package com.example.tourism_service.controller;

import com.example.tourism_service.dto.SignatureIdsRequest;
import com.example.tourism_service.service.binary.BinarySignatureExportService;
import com.example.tourism_service.service.binary.BinarySignaturePackage;
import com.example.tourism_service.service.binary.MultipartMixedResponseFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/binary/signatures")
public class BinarySignatureController {

    private final BinarySignatureExportService binarySignatureExportService;
    private final MultipartMixedResponseFactory multipartMixedResponseFactory;

    public BinarySignatureController(BinarySignatureExportService binarySignatureExportService,
                                     MultipartMixedResponseFactory multipartMixedResponseFactory) {
        this.binarySignatureExportService = binarySignatureExportService;
        this.multipartMixedResponseFactory = multipartMixedResponseFactory;
    }

    @GetMapping("/full")
    public ResponseEntity<MultiValueMap<String, Object>> getFull() {
        BinarySignaturePackage binaryPackage = binarySignatureExportService.buildFullPackage();
        return createResponse(binaryPackage);
    }

    @GetMapping("/increment")
    public ResponseEntity<MultiValueMap<String, Object>> getIncrement(
            @RequestParam("since")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant since
    ) {
        BinarySignaturePackage binaryPackage = binarySignatureExportService.buildIncrementPackage(since);
        return createResponse(binaryPackage);
    }

    @PostMapping("/by-ids")
    public ResponseEntity<MultiValueMap<String, Object>> getByIds(@RequestBody SignatureIdsRequest request) {
        BinarySignaturePackage binaryPackage = binarySignatureExportService.buildByIdsPackage(request.getIds());
        return createResponse(binaryPackage);
    }

    private ResponseEntity<MultiValueMap<String, Object>> createResponse(BinarySignaturePackage binaryPackage) {
        HttpEntity<MultiValueMap<String, Object>> multipartEntity =
                multipartMixedResponseFactory.create(binaryPackage);

        return ResponseEntity.ok()
                .headers(multipartEntity.getHeaders())
                .body(multipartEntity.getBody());
    }
}