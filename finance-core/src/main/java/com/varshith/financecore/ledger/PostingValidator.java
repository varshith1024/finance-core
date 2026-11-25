package com.varshith.financecore.ledger;

import java.util.List;

public final class PostingValidator {

    private PostingValidator() {}

    public static void validate(List<PostingLine> lines) {
        if (lines == null || lines.size() < 2) {
            throw new LedgerValidationException("A journal needs at least 2 entries");
        }
        long debits = 0;
        long credits = 0;
        for (PostingLine l : lines) {
            if (l.accountId() == null || l.direction() == null) {
                throw new LedgerValidationException("Each entry needs an account and a direction");
            }
            if (l.amountMinor() <= 0) {
                throw new LedgerValidationException("Entry amounts must be positive");
            }
            // addExact throws instead of silently overflowing
            if (l.direction() == Direction.DEBIT) debits = Math.addExact(debits, l.amountMinor());
            else credits = Math.addExact(credits, l.amountMinor());
        }
        if (debits != credits) {
            throw new LedgerValidationException(
                "Unbalanced journal: debits=" + debits + " credits=" + credits);
        }
    }
}