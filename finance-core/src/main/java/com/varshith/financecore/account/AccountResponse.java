package com.varshith.financecore.account;

import java.util.UUID;

public record AccountResponse(UUID id, String accountType, String currency, String status) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getAccountType(), a.getCurrency(), a.getStatus());
    }
}