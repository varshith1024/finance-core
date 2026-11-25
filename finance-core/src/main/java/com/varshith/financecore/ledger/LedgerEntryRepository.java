package com.varshith.financecore.ledger;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    // balance = credits - debits, computed from the ledger (the source of truth)
    @Query("""
           select coalesce(sum(case when e.direction = :credit then e.amountMinor else -e.amountMinor end), 0)
           from LedgerEntry e where e.accountId = :accountId
           """)
    Long signedSum(@Param("accountId") UUID accountId, @Param("credit") Direction credit);

    default long balanceOf(UUID accountId) {
        return signedSum(accountId, Direction.CREDIT);
    }

    // newest first, one page at a time (Pageable adds LIMIT/OFFSET)
    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
}