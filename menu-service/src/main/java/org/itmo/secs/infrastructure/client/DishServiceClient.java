package org.itmo.secs.infrastructure.client;

import org.itmo.secs.domain.model.dto.DishDto;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

@Component
@ReactiveFeignClient(
        name = "dish-service",
        configuration = DishServiceClientConfig.class
)
@ResponseBody
public interface DishServiceClient {
    @GetMapping("/dish")
    Mono<DishDto> getById(@PathVariable("id") Long id);
}

