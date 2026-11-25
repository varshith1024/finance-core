package com.varshith.financecore.transfer;

import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id, String status, UUID sourceAccountId, UUID destinationAccountId,
        long amountMinor, String currency, String failureReason,
        UUID ledgerTransactionId, Instant createdAt) {

    public static TransferResponse from(Transfer t) {
        return new TransferResponse(t.getId(), t.getStatus(), t.getSourceAccountId(),
                t.getDestinationAccountId(), t.getAmountMinor(), t.getCurrency(),
                t.getFailureReason(), t.getLedgerTransactionId(), t.getCreatedAt());
    }
}