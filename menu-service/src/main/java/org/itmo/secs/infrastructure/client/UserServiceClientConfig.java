package org.itmo.secs.infrastructure.client;

import feign.codec.ErrorDecoder;
import org.itmo.secs.exception.AccessDeniedException;
import org.itmo.secs.exception.ItemNotFoundException;
import org.itmo.secs.exception.ServiceUnavailableException;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

public class UserServiceClientConfig {
    public ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Bean
    public ErrorDecoder errorDecoder() {
        return (s, response) -> {
                HttpStatus status = HttpStatus.valueOf(response.status());

            return switch (status) {
                case NOT_FOUND -> new ItemNotFoundException("User was not found");
                case FORBIDDEN -> new AccessDeniedException("FORBIDDEN");
                case SERVICE_UNAVAILABLE -> new ServiceUnavailableException("User-service unavailable");
                default -> defaultDecoder.decode(s, response);
            };
        };
    }
}
