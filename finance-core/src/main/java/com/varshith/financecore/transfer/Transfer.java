package com.varshith.financecore.transfer;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "failure_reason", length = 50)
    private String failureReason;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "ledger_transaction_id")
    private UUID ledgerTransactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transfer() {}

    public static Transfer pending(UUID source, UUID destination, long amountMinor,
                                   String idempotencyKey, UUID createdBy) {
        Transfer t = new Transfer();
        Instant now = Instant.now();
        t.sourceAccountId = source;
        t.destinationAccountId = destination;
        t.amountMinor = amountMinor;
        t.currency = "INR";
        t.status = "PENDING";
        t.idempotencyKey = idempotencyKey;
        t.createdByUserId = createdBy;
        t.createdAt = now;
        t.updatedAt = now;
        return t;
    }

    public void complete(UUID ledgerTransactionId) {
        requirePending();
        this.status = "COMPLETED";
        this.ledgerTransactionId = ledgerTransactionId;
        this.updatedAt = Instant.now();
    }

    public void fail(String reason) {
        requirePending();
        this.status = "FAILED";
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    private void requirePending() {
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Transfer " + id + " is " + status + ", not PENDING");
        }
    }

    public UUID getId() { return id; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getDestinationAccountId() { return destinationAccountId; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public UUID getLedgerTransactionId() { return ledgerTransactionId; }
    public Instant getCreatedAt() { return createdAt; }
}