package org.itmo.user.accounter.utils.exceptions;

import org.itmo.user.accounter.model.dto.ErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class ValidationExceptionsHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ErrorDto> handleValidationExceptions(
            WebExchangeBindException e) {
        return Mono.just(
                new ErrorDto("Validation error. Bad request data values")
        );
    }
}
