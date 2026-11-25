package com.varshith.financecore.ledger;

import com.varshith.financecore.account.Account;
import com.varshith.financecore.account.AccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    public static final UUID SYSTEM_FUNDING_ACCOUNT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final LedgerTransactionRepository transactions;
    private final LedgerEntryRepository entries;
    private final AccountRepository accounts;

    public LedgerService(LedgerTransactionRepository transactions,
                         LedgerEntryRepository entries,
                         AccountRepository accounts) {
        this.transactions = transactions;
        this.entries = entries;
        this.accounts = accounts;
    }

    /** Posts a balanced journal atomically: all entries or none. */
    @Transactional
    public UUID post(String reference, String type, String description, List<PostingLine> lines) {
        PostingValidator.validate(lines);

        Set<UUID> ids = lines.stream().map(PostingLine::accountId).collect(Collectors.toSet());
        Map<UUID, Account> found = accounts.findAllById(ids).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        for (UUID id : ids) {
            Account a = found.get(id);
            if (a == null) throw new LedgerValidationException("Account not found: " + id);
            if (!"ACTIVE".equals(a.getStatus())) throw new LedgerValidationException("Account not active: " + id);
            if (!"INR".equals(a.getCurrency())) throw new LedgerValidationException("Currency mismatch: " + id);
        }

        LedgerTransaction txn = transactions.save(LedgerTransaction.posted(reference, type, description));
        for (PostingLine l : lines) {
            entries.save(new LedgerEntry(txn.getId(), l.accountId(), l.direction(), l.amountMinor()));
        }
        return txn.getId();   // DB re-checks balance at commit (deferred trigger)
    }

    public long balanceOf(UUID accountId) {
        return entries.balanceOf(accountId);
    }

    /** Ledger entries for one account, newest first, with their journal's type and reference. */
    @Transactional(readOnly = true)
    public List<ActivityItem> activity(UUID accountId, int page, int size) {
        List<LedgerEntry> rows = entries.findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(page, size));
        Set<UUID> txnIds = rows.stream().map(LedgerEntry::getLedgerTransactionId).collect(Collectors.toSet());
        // one query for all journals instead of one per row (avoids the N+1 problem)
        Map<UUID, LedgerTransaction> txns = transactions.findAllById(txnIds).stream()
                .collect(Collectors.toMap(LedgerTransaction::getId, Function.identity()));
        return rows.stream().map(e -> {
            LedgerTransaction t = txns.get(e.getLedgerTransactionId());
            return new ActivityItem(e.getId(), t.getId(), t.getReference(), t.getTransactionType(),
                    t.getDescription(), e.getDirection(), e.getAmountMinor(), e.getCreatedAt());
        }).toList();
    }
}