package com.varshith.financecore.ledger;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ledger_transaction_id", nullable = false)
    private UUID ledgerTransactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private Direction direction;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {}

    public LedgerEntry(UUID ledgerTransactionId, UUID accountId, Direction direction, long amountMinor) {
        this.ledgerTransactionId = ledgerTransactionId;
        this.accountId = accountId;
        this.direction = direction;
        this.amountMinor = amountMinor;
        this.currency = "INR";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getLedgerTransactionId() { return ledgerTransactionId; }
    public UUID getAccountId() { return accountId; }
    public Direction getDirection() { return direction; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}