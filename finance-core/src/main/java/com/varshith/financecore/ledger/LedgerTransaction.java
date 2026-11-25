package com.varshith.financecore.ledger;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable   // Hibernate will never issue UPDATE for this entity
@Table(name = "ledger_transactions")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String reference;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    @Column(nullable = false, length = 20)
    private String status;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "reversal_of_id")
    private UUID reversalOfId;

    protected LedgerTransaction() {}

    public static LedgerTransaction posted(String reference, String type, String description) {
        LedgerTransaction t = new LedgerTransaction();
        Instant now = Instant.now();
        t.reference = reference;
        t.transactionType = type;
        t.status = "POSTED";
        t.currency = "INR";
        t.description = description;
        t.createdAt = now;
        t.postedAt = now;
        return t;
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getTransactionType() { return transactionType; }
    public String getStatus() { return status; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public Instant getPostedAt() { return postedAt; }
    public UUID getReversalOfId() { return reversalOfId; }
}