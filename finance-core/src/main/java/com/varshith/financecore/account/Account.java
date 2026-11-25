package com.varshith.financecore.account;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Account() {}

    public static Account wallet(UUID ownerUserId) {
        Account a = new Account();
        a.ownerUserId = ownerUserId;
        a.accountType = "WALLET";
        a.currency = "INR";
        a.status = "ACTIVE";
        a.createdAt = Instant.now();
        return a;
    }

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getAccountType() { return accountType; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}