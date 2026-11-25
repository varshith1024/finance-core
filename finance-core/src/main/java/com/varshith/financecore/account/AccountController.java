package com.varshith.financecore.account;

import com.varshith.financecore.ledger.ActivityItem;
import com.varshith.financecore.ledger.LedgerService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountRepository accounts;
    private final LedgerService ledger;

    public AccountController(AccountRepository accounts, LedgerService ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    @GetMapping
    public List<AccountResponse> myAccounts(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return accounts.findByOwnerUserId(userId).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Account account = ownedAccount(id, jwt);
        return BalanceResponse.of(account.getId(), account.getCurrency(), ledger.balanceOf(account.getId()));
    }

    /** GET /api/v1/accounts/{id}/transactions?page=0&size=20 */
    @GetMapping("/{id}/transactions")
    public List<ActivityItem> transactions(@PathVariable UUID id,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @AuthenticationPrincipal Jwt jwt) {
        Account account = ownedAccount(id, jwt);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);   // never let a client ask for a million rows
        return ledger.activity(account.getId(), safePage, safeSize);
    }

    /** 404 (not 403) for someone else's account, so they can't even learn it exists. */
    private Account ownedAccount(UUID id, Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return accounts.findById(id)
                .filter(a -> userId.equals(a.getOwnerUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }
}