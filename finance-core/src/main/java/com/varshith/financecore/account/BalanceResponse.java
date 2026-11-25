package com.varshith.financecore.account;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(UUID accountId, String currency, long balanceMinor, String balance) {
    public static BalanceResponse of(UUID accountId, String currency, long minor) {
        // 50000 paise -> "500.00" (exact, no floating point)
        return new BalanceResponse(accountId, currency, minor, BigDecimal.valueOf(minor, 2).toPlainString());
    }
}