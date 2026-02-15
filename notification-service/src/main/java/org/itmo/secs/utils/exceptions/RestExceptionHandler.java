package org.itmo.secs.utils.exceptions;

import io.jsonwebtoken.ExpiredJwtException;
import org.itmo.secs.model.dto.ErrorDto;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestExceptionHandler {

    @ExceptionHandler({ExpiredJwtException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorDto> handleExpiredJwtException(ExpiredJwtException e) {
        return Mono.just(new ErrorDto("Token has expired"));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ErrorDto> handleValidationExceptions(
            WebExchangeBindException e) {
        return Mono.just(
                new ErrorDto("Validation error. Bad request data values")
        );
    }
}
