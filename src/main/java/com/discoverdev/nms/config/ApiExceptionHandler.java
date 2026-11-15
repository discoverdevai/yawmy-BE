package com.discoverdev.nms.config;

import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
 * act on. ResponseStatusException is deliberate and user-facing, and continues
 * to flow through Spring's default handling untouched. Validation errors are
 * rewritten from "Validation failed for object='…'" to the first field problem.
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

    private static final Pattern INDEXED = Pattern.compile("(\\w+)\\[(\\d+)]");
    private static final Map<String, String> FIELD_NAMES = Map.of(
        "children", "child",
        "guardians", "guardian",
        "pickups", "pickup contact",
        "dob", "date of birth",
        "fullName", "name"
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> onInvalidRequest(
        MethodArgumentNotValidException e, HttpServletRequest request
    ) {
        FieldError error = e.getBindingResult().getFieldError();
        String message = error != null
            ? describeField(error.getField()) + " " + error.getDefaultMessage()
            : "Some of the details are invalid. Please check them and try again.";
        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    private static String describeField(String path) {
        StringBuilder out = new StringBuilder();
        for (String part : path.split("\\.")) {
            Matcher indexed = INDEXED.matcher(part);
            String name = indexed.matches() ? indexed.group(1) : part;
            String words = FIELD_NAMES.getOrDefault(
                name, name.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase());
            if (indexed.matches()) {
                words += " " + (Integer.parseInt(indexed.group(2)) + 1);
            }
            if (!out.isEmpty()) out.append(' ');
            out.append(words);
        }
        return Character.toUpperCase(out.charAt(0)) + out.substring(1);
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
