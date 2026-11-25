package com.varshith.financecore.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Runs first on every request:
 *  1. gives the request a correlation ID (put in MDC, so EVERY log line of this request carries it)
 *  2. returns it in the X-Correlation-Id response header
 *  3. logs one access line with method, route, status and duration (for latency dashboards)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // before Spring Security, so 401s are logged too
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger("http.access");
    // only accept safe incoming IDs; anything else could inject fake text into our logs
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final Pattern UUID_IN_PATH =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String incoming = req.getHeader(HEADER);
        String id = (incoming != null && SAFE_ID.matcher(incoming).matches()) ? incoming : UUID.randomUUID().toString();
        MDC.put("correlationId", id);
        res.setHeader(HEADER, id);
        long start = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            // /api/v1/transfers/9a1f... -> /api/v1/transfers/{id}, so Splunk can group latency per endpoint
            String route = UUID_IN_PATH.matcher(req.getRequestURI()).replaceAll("{id}");
            log.atInfo()
                    .addKeyValue("action", "http.request")
                    .addKeyValue("method", req.getMethod())
                    .addKeyValue("route", route)
                    .addKeyValue("status", res.getStatus())
                    .addKeyValue("durationMs", ms)
                    .log("{} {} -> {} in {} ms", req.getMethod(), route, res.getStatus(), ms);
            MDC.remove("correlationId");
        }
    }
}