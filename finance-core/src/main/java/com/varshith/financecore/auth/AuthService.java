package com.varshith.financecore.auth;

import com.varshith.financecore.account.Account;
import com.varshith.financecore.account.AccountRepository;
import com.varshith.financecore.user.User;
import com.varshith.financecore.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, AccountRepository accounts,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        // role is always CUSTOMER, never taken from the client
        User user = users.save(new User(email, passwordEncoder.encode(req.password()), "CUSTOMER"));
        accounts.save(Account.wallet(user.getId()));   // same transaction: both or neither
    }

            public String login(LoginRequest req) {
        var user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()));
        if (user.isEmpty()) {
            // never log the password; mask the email
            log.atWarn()
                    .addKeyValue("action", "auth.login_failed")
                    .addKeyValue("emailMasked", mask(req.email()))
                    .log("Login failed");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        log.atInfo()
                .addKeyValue("action", "auth.login_succeeded")
                .addKeyValue("userId", user.get().getId())
                .log("Login succeeded");
        return jwtService.issue(user.get());
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        return at > 1 ? email.substring(0, 2) + "****" + email.substring(at) : "****";
    }
}