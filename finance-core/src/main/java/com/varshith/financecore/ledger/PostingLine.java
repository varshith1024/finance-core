package com.varshith.financecore.ledger;

import java.util.UUID;

public record PostingLine(UUID accountId, Direction direction, long amountMinor) {}