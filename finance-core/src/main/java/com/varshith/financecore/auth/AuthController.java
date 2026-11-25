package com.varshith.financecore.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public static final String COOKIE_NAME = "access_token";

    private final AuthService authService;
    private final JwtService jwtService;
    private final boolean cookieSecure;

    public AuthController(AuthService authService, JwtService jwtService,
                          @Value("${app.cookie.secure}") boolean cookieSecure) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req);
        ResponseCookie cookie = buildCookie(token, jwtService.getTtlSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse("Logged in", jwtService.getTtlSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // same cookie name, empty value, maxAge 0 = browser deletes it
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, buildCookie("", 0).toString())
                .build();
    }

    private ResponseCookie buildCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)          // JS cannot read it
                .secure(cookieSecure)    // HTTPS only when deployed
                .sameSite("Strict")      // not sent on cross-site requests (CSRF protection)
                .path("/")
                .maxAge(maxAgeSeconds)   // expires with the JWT
                .build();
    }
}