package org.itmo.user.accounter.utils.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class ValidationExceptionsHandler {
    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected Mono<ResponseEntity<String>> handleConstraintViolationException(
            ConstraintViolationException e) {
        return Mono.just(new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST));
    }
}
