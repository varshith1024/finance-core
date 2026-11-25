package com.varshith.financecore.transfer;

import com.varshith.financecore.account.Account;
import com.varshith.financecore.account.AccountRepository;
import com.varshith.financecore.ledger.Direction;
import com.varshith.financecore.ledger.LedgerService;
import com.varshith.financecore.ledger.PostingLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    public record Result(int httpStatus, boolean replayed, TransferResponse body) {}

    private final IdempotencyRecordRepository idempotency;
    private final TransferRepository transfers;
    private final AccountRepository accounts;
    private final LedgerService ledger;
    private final OutboxEventRepository outbox;

    public TransferService(IdempotencyRecordRepository idempotency, TransferRepository transfers,
                           AccountRepository accounts, LedgerService ledger, OutboxEventRepository outbox) {
        this.idempotency = idempotency;
        this.transfers = transfers;
        this.accounts = accounts;
        this.ledger = ledger;
        this.outbox = outbox;
    }

    @Transactional
    public Result create(UUID userId, String idempotencyKey, TransferRequest req) {
        String key = validateKey(idempotencyKey);
        String scope = userId.toString();
        String hash = hash(req);

        if (idempotency.tryInsert(scope, key, hash) == 0) {
            return replay(userId, scope, key, hash);
        }
        IdempotencyRecord record = idempotency.findByScopeAndKey(scope, key).orElseThrow();

        Account source = accounts.findByIdForUpdate(req.sourceAccountId())
                .filter(a -> userId.equals(a.getOwnerUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found"));

        if (req.sourceAccountId().equals(req.destinationAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer to the same account");
        }
        Account destination = accounts.findById(req.destinationAccountId())
                .filter(a -> "WALLET".equals(a.getAccountType()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found"));
        if (!"ACTIVE".equals(source.getStatus()) || !"ACTIVE".equals(destination.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Account is not active");
        }

        Transfer transfer = transfers.saveAndFlush(Transfer.pending(
                source.getId(), destination.getId(), req.amountMinor(), key, userId));

        long available = ledger.balanceOf(source.getId());
        int status;
        if (available < req.amountMinor()) {
            transfer.fail("INSUFFICIENT_FUNDS");
            status = 422;
            log.atWarn()
                    .addKeyValue("action", "transfer.failed")
                    .addKeyValue("reason", "INSUFFICIENT_FUNDS")
                    .addKeyValue("transferId", transfer.getId())
                    .addKeyValue("userId", userId)
                    .addKeyValue("amountMinor", req.amountMinor())
                    .log("Transfer failed: insufficient funds");
        } else {
            UUID ledgerTxnId = ledger.post("TRF-" + transfer.getId(), "TRANSFER", "Wallet transfer",
                    List.of(new PostingLine(source.getId(), Direction.DEBIT, req.amountMinor()),
                            new PostingLine(destination.getId(), Direction.CREDIT, req.amountMinor())));
            transfer.complete(ledgerTxnId);
            outbox.save(new OutboxEvent("TRANSFER", transfer.getId(), "TransferCompleted",
                    "{\"transferId\":\"" + transfer.getId() + "\",\"amountMinor\":" + req.amountMinor()
                    + ",\"currency\":\"INR\"}"));
            status = 201;
            log.atInfo()
                    .addKeyValue("action", "transfer.completed")
                    .addKeyValue("transferId", transfer.getId())
                    .addKeyValue("ledgerTransactionId", ledgerTxnId)
                    .addKeyValue("userId", userId)
                    .addKeyValue("amountMinor", req.amountMinor())
                    .log("Transfer completed");
        }

        record.complete(status, transfer.getId());
        return new Result(status, false, TransferResponse.from(transfer));
    }

    @Transactional(readOnly = true)
    public TransferResponse get(UUID userId, UUID transferId) {
        return transfers.findById(transferId)
                .filter(t -> userId.equals(t.getCreatedByUserId()))
                .map(TransferResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found"));
    }

    private Result replay(UUID userId, String scope, String key, String hash) {
        IdempotencyRecord existing = idempotency.findByScopeAndKey(scope, key).orElseThrow();
        if (!existing.getRequestHash().equals(hash)) {
            log.atWarn()
                    .addKeyValue("action", "transfer.idempotency_conflict")
                    .addKeyValue("userId", userId)
                    .log("Idempotency key reused with a different request");
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency-Key was already used with a different request");
        }
        if (!"COMPLETED".equals(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request with this key is still in progress");
        }
        Transfer original = transfers.findById(existing.getResourceId()).orElseThrow();
        log.atInfo()
                .addKeyValue("action", "transfer.replayed")
                .addKeyValue("transferId", original.getId())
                .addKeyValue("userId", userId)
                .log("Idempotent replay: returned original transfer, no money moved");
        return new Result(existing.getResponseCode(), true, TransferResponse.from(original));
    }

    private static String validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key header is required (max 100 chars)");
        }
        return key.trim();
    }

    private static String hash(TransferRequest req) {
        String canonical = req.sourceAccountId() + "|" + req.destinationAccountId() + "|" + req.amountMinor() + "|INR";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}