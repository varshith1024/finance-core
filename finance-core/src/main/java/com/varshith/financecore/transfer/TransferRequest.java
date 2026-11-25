package com.varshith.financecore.transfer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID destinationAccountId,
        @Positive @Max(10_000_000) long amountMinor) {}   // max ₹1,00,000 per transfer