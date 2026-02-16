package org.itmo.secs.infrastructure.advices;

import jakarta.validation.ConstraintViolationException;
import org.itmo.secs.model.dto.ErrorDto;
import org.itmo.secs.infrastructure.exceptions.DataIntegrityViolationException;
import org.itmo.secs.infrastructure.exceptions.ItemNotFoundException;
import org.itmo.secs.infrastructure.exceptions.ReExecutionException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionTranslator {
    @ExceptionHandler(ItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDto processItemNotFoundException(ItemNotFoundException ex) {
        return new ErrorDto(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto processItemNotFoundException(DataIntegrityViolationException ex) {
        return new ErrorDto(ex.getMessage());
    }

    @ExceptionHandler(ReExecutionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto processItemNotFoundException(ReExecutionException ex) {
        return new ErrorDto(ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto processConstraintViolationException(ConstraintViolationException ex) {
        return new ErrorDto("Field validation failed");
    }
}
