package org.itmo.secs.client;

import org.itmo.secs.model.dto.DishDto;
import org.itmo.secs.utils.exceptions.ServiceUnavailableException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DishServiceClientFallback implements DishServiceClient {

    @Override
    public Mono<DishDto> getById(Long id) {
        return Mono.error(new ServiceUnavailableException("Dish Service is unavailable now, cannot calculate menu's CCPF"));
    }
}
