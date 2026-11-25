package com.varshith.financecore.transfer;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        TransferService.Result r = service.create(userId, idempotencyKey, req);
        return ResponseEntity.status(r.httpStatus())
                .header("Idempotent-Replayed", String.valueOf(r.replayed()))
                .body(r.body());
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.get(UUID.fromString(jwt.getSubject()), id);
    }
}