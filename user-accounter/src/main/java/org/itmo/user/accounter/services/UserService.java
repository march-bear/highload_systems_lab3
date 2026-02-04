package org.itmo.user.accounter.services;

import org.itmo.user.accounter.utils.exceptions.DataIntegrityViolationException;
import org.itmo.user.accounter.utils.exceptions.ItemNotFoundException;
import org.itmo.user.accounter.repositories.UserRepository;
import org.itmo.user.accounter.model.entities.User;
import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRep;

    @Transactional
    public Mono<User> save(User user) {
        return userRep.findByName(user.getName())
                .doOnNext(x -> {
                    throw new DataIntegrityViolationException("User with name " + user.getName() + " already exists" );
                })
                .switchIfEmpty(userRep.save(user));
    }

    @Transactional
    public Mono<User> update(User user) {
        return userRep.findById(user.getId())
                .switchIfEmpty(Mono.error(new ItemNotFoundException("User with id " + user.getId() + " was not found")))
                .flatMap(existingUser -> {
                    existingUser.setName(user.getName());
                    return userRep.save(existingUser);
                });
    }

    @Transactional
    public Mono<Void> deleteById(Long id) {
        return userRep.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("User with id " + id + " was not found")))
                .flatMap(user -> userRep.deleteById(id));
    }

    public Mono<User> findByName(String name) {
        return userRep.findByName(name);
    }

    public Mono<User> findById(Long id) {
        return userRep.findById(id);
    }
}