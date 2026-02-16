package org.itmo.secs.exception.advices;

import jakarta.validation.ConstraintViolationException;
import org.itmo.secs.domain.model.dto.ErrorDto;
import org.itmo.secs.exception.AccessDeniedException;
import org.itmo.secs.exception.DataIntegrityViolationException;
import org.itmo.secs.exception.ItemNotFoundException;
import org.itmo.secs.exception.ServiceUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionTranslator {
    @ExceptionHandler(ItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorDto processItemNotFoundException(ItemNotFoundException ex) {
        return new ErrorDto(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDto processItemNotFoundException(DataIntegrityViolationException ex) {
        return new ErrorDto(ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDto processConstraintViolationException(ConstraintViolationException ex) {
        return new ErrorDto("Field validation failed");
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ResponseBody
    public ErrorDto processServiceUnavailableException(ServiceUnavailableException ex) {
        return new ErrorDto(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorDto processServiceUnavailableException(AccessDeniedException ex) {
        return new ErrorDto(ex.getMessage());
    }
}
