package org.itmo.secs.client;

import org.itmo.secs.model.dto.UserDto;
import org.itmo.secs.utils.exceptions.ServiceUnavailableException;
import reactor.core.publisher.Mono;

public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public Mono<UserDto> getByName(String username) {
        return Mono.error(new ServiceUnavailableException("User Service is unavailable now, cannot find " + username + "'s menus"));
    }
}
