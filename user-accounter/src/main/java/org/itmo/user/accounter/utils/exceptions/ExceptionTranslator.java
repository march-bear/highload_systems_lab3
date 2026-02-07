package org.itmo.user.accounter.utils.exceptions;

import org.itmo.user.accounter.model.dto.ErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class ExceptionTranslator {
    @ExceptionHandler(ItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorDto> processItemNotFoundException(ItemNotFoundException ex) {
        return Mono.just(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorDto> processItemNotFoundException(DataIntegrityViolationException ex) {
        return Mono.just(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(AssigningAdminViaAPIException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorDto> processItemNotFoundException(AssigningAdminViaAPIException ex) {
        return Mono.just(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorDto> processItemNotFoundException(BadCredentialsException ex) {
        return Mono.just(new ErrorDto("Bad username or password"));
    }
}
