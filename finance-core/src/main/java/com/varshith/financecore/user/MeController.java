package com.varshith.financecore.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

/** GET /api/v1/me: who is logged in. The frontend uses it to show the user and check the session. */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    public record MeResponse(UUID id, String email, String role) {}

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());   // identity comes from the signed token only
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        return new MeResponse(u.getId(), u.getEmail(), u.getRole());
    }
}