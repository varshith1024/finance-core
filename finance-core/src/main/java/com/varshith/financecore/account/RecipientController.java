package com.varshith.financecore.account;

import com.varshith.financecore.user.User;
import com.varshith.financecore.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

/**
 * POST /api/v1/recipients/resolve  { "email": "friend@test.com" }
 * Turns a recipient's email into their wallet id so users don't paste UUIDs.
 * POST (not GET) so the email never ends up in URLs or access logs.
 */
@RestController
@RequestMapping("/api/v1/recipients")
public class RecipientController {

    public record ResolveRequest(@NotBlank @Email String email) {}
    public record RecipientResponse(UUID accountId, String maskedEmail) {}

    private final UserRepository users;
    private final AccountRepository accounts;

    public RecipientController(UserRepository users, AccountRepository accounts) {
        this.users = users;
        this.accounts = accounts;
    }

    @PostMapping("/resolve")
    public RecipientResponse resolve(@Valid @RequestBody ResolveRequest req, @AuthenticationPrincipal Jwt jwt) {
        UUID me = UUID.fromString(jwt.getSubject());
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No FinCore user with that email"));
        if (user.getId().equals(me)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't send money to yourself");
        }
        Account wallet = accounts.findByOwnerUserId(user.getId()).stream()
                .filter(a -> "WALLET".equals(a.getAccountType()) && "ACTIVE".equals(a.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient has no active wallet"));
        return new RecipientResponse(wallet.getId(), mask(user.getEmail()));
    }

    /** "rahul@test.com" -> "ra****@test.com": confirm the person without exposing the full email. */
    static String mask(String email) {
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "****" + email.substring(at);
    }
}