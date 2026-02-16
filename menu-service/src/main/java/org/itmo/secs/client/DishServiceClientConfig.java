package org.itmo.secs.client;

import feign.codec.ErrorDecoder;
import org.itmo.secs.infrastructure.exceptions.AccessDeniedException;
import org.itmo.secs.infrastructure.exceptions.ItemNotFoundException;
import org.itmo.secs.infrastructure.exceptions.ServiceUnavailableException;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

public class DishServiceClientConfig {
    public ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Bean
    public ErrorDecoder errorDecoder() {
        return (s, response) -> {
                HttpStatus status = HttpStatus.valueOf(response.status());

            return switch (status) {
                case NOT_FOUND -> new ItemNotFoundException("Dish was not found");
                case FORBIDDEN -> new AccessDeniedException("FORBIDDEN");
                case SERVICE_UNAVAILABLE -> new ServiceUnavailableException("Dish-service unavailable");
                default -> defaultDecoder.decode(s, response);
            };
        };
    }
}
