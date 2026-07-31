package com.discoverdev.nms.config;

import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Keeps database internals out of API responses.
 *
 * `server.error.include-message: always` is on so that deliberate messages
 * ("Email not found or incorrect email") reach the apps. The side effect is that
 * an *accidental* failure is forwarded just as faithfully — a constraint
 * violation carries the full INSERT statement, the table name and every column
 * name, and the apps render whatever text they are given. A nursery admin has no
 * use for that and it describes the schema to anyone watching.
 *
 * So: log the real cause with the stack trace, answer with something a user can
 * act on. Only infrastructure-level failures are handled here —
 * ResponseStatusException and validation errors are deliberate, already
 * user-facing, and continue to flow through Spring's default handling untouched.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({
        DataAccessException.class,   // Spring-translated JDBC/JPA failures
        TransactionException.class,  // commit-time failures (TransactionSystemException)
        PersistenceException.class   // raw Hibernate failures that escaped translation
    })
    public ResponseEntity<Map<String, Object>> onDatabaseFailure(
        RuntimeException e, HttpServletRequest request
    ) {
        log.error(
            "Database failure on {} {} — {}",
            request.getMethod(), request.getRequestURI(), e.getMessage(), e
        );
        return respond(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Something went wrong on our side. Please try again.",
            request
        );
    }

    private ResponseEntity<Map<String, Object>> respond(
        HttpStatus status, String message, HttpServletRequest request
    ) {
        // Same shape Spring's default error page produces, so the apps read it
        // with the `data.message` they already use.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
