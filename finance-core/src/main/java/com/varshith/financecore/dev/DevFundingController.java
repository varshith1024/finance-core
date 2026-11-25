package com.varshith.financecore.dev;

import com.varshith.financecore.account.AccountRepository;
import com.varshith.financecore.ledger.Direction;
import com.varshith.financecore.ledger.LedgerService;
import com.varshith.financecore.ledger.PostingLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Profile("dev")   // this controller does not exist unless the app runs with the dev profile
@RestController
@RequestMapping("/api/v1/dev")
public class DevFundingController {

    public record FundRequest(@Positive @Max(10_000_000) long amountMinor) {}   // max ₹1,00,000

    private final AccountRepository accounts;
    private final LedgerService ledger;

    public DevFundingController(AccountRepository accounts, LedgerService ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    @PostMapping("/fund")
    public Map<String, Object> fund(@Valid @RequestBody FundRequest req, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID walletId = accounts.findByOwnerUserId(userId).stream()
                .filter(a -> "WALLET".equals(a.getAccountType()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"))
                .getId();

        UUID txnId = ledger.post(
                "FUND-" + UUID.randomUUID(), "TEST_FUNDING", "Dev-only opening balance",
                List.of(new PostingLine(LedgerService.SYSTEM_FUNDING_ACCOUNT_ID, Direction.DEBIT, req.amountMinor()),
                        new PostingLine(walletId, Direction.CREDIT, req.amountMinor())));

        return Map.of("ledgerTransactionId", txnId, "walletId", walletId, "amountMinor", req.amountMinor());
    }
}