package com.varshith.financecore.transfer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String scope;

    @Column(name = "idem_key", nullable = false, length = 100)
    private String idemKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdempotencyRecord() {}

    public void complete(int responseCode, UUID resourceId) {
        this.status = "COMPLETED";
        this.responseCode = responseCode;
        this.resourceId = resourceId;
        this.updatedAt = Instant.now();
    }

    public String getRequestHash() { return requestHash; }
    public String getStatus() { return status; }
    public Integer getResponseCode() { return responseCode; }
    public UUID getResourceId() { return resourceId; }
}