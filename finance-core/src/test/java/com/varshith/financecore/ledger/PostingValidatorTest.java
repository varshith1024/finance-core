package com.varshith.financecore.ledger;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PostingValidatorTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void balancedJournalIsAccepted() {
        assertDoesNotThrow(() -> PostingValidator.validate(List.of(
                new PostingLine(a, Direction.DEBIT, 100000),
                new PostingLine(b, Direction.CREDIT, 100000))));
    }

    @Test
    void unbalancedJournalIsRejected() {
        assertThrows(LedgerValidationException.class, () -> PostingValidator.validate(List.of(
                new PostingLine(a, Direction.DEBIT, 100000),
                new PostingLine(b, Direction.CREDIT, 99999))));
    }

    @Test
    void singleEntryIsRejected() {
        assertThrows(LedgerValidationException.class, () -> PostingValidator.validate(List.of(
                new PostingLine(a, Direction.DEBIT, 100))));
    }

    @Test
    void zeroOrNegativeAmountIsRejected() {
        assertThrows(LedgerValidationException.class, () -> PostingValidator.validate(List.of(
                new PostingLine(a, Direction.DEBIT, 0),
                new PostingLine(b, Direction.CREDIT, 0))));
    }
}