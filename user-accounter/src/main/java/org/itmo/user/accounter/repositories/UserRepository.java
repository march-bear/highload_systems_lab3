package org.itmo.user.accounter.repositories;

import org.itmo.user.accounter.model.entities.User;

import org.itmo.user.accounter.model.entities.enums.UserRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByUsername(String username);
    Flux<User> findAllByRole(UserRole role);
}
