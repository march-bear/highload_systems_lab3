package org.itmo.secs.infrastructure.client;

import org.itmo.secs.domain.model.dto.UserDto;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

@Component
@ReactiveFeignClient(
        name = "user-accounter",
        configuration = UserServiceClientConfig.class
)
@ResponseBody
public interface UserServiceClient {
    @GetMapping("/user")
    Mono<UserDto> getByName(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("name") String username
    );
}

