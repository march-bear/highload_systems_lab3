package org.itmo.secs.client;

import org.itmo.secs.model.dto.UserDto;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

@Component
@ReactiveFeignClient(name = "user-accounter", fallback = UserServiceClientFallback.class)
@ResponseBody
public interface UserServiceClient {
    @GetMapping("/user")
    Mono<UserDto> getByName(@PathVariable("name") String username);
}

