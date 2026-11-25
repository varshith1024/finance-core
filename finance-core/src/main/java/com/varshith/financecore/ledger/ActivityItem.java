package com.varshith.financecore.ledger;

import java.time.Instant;
import java.util.UUID;

/** One ledger entry on an account, joined with its journal, for the activity screen. */
public record ActivityItem(
        UUID entryId,
        UUID ledgerTransactionId,
        String reference,
        String type,
        String description,
        Direction direction,
        long amountMinor,
        Instant createdAt) {}