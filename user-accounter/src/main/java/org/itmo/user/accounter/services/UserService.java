package org.itmo.user.accounter.services;

import org.itmo.user.accounter.model.entities.enums.UserRole;
import org.itmo.user.accounter.utils.exceptions.AssigningAdminViaAPIException;
import org.itmo.user.accounter.utils.exceptions.DataIntegrityViolationException;
import org.itmo.user.accounter.utils.exceptions.ItemNotFoundException;
import org.itmo.user.accounter.repositories.UserRepository;
import org.itmo.user.accounter.model.entities.User;
import lombok.AllArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class UserService implements ReactiveUserDetailsService {
    private UserRepository userRep;

    @Transactional
    public Mono<User> create(User user) {
        return userRep.findByUsername(user.getUsername())
                .doOnNext(x -> {
                    throw new DataIntegrityViolationException("User with name " + user.getUsername() + " already exists" );
                })
                .switchIfEmpty(userRep.save(user));
    }

    @Transactional
    public Mono<User> updateRole(Long id, UserRole role) {
        if (role == UserRole.ADMIN) {
            return Mono.error(new AssigningAdminViaAPIException("ADMIN cannot be assigned via web API"));
        }

        return userRep.findById(id)
                .switchIfEmpty(Mono.error(new DataIntegrityViolationException("User with id " + id + " was not found")))
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .switchIfEmpty(Mono.error(new AssigningAdminViaAPIException("ADMIN cannot be unassigned via web API")))
                .flatMap(user -> userRep.save(
                        User.builder()
                                .id(id)
                                .role(role)
                                .password(user.getPassword())
                                .username(user.getUsername())
                                .build()
                ));
    }

    @Transactional
    public Mono<Void> deleteById(Long id) {
        return userRep.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("User with id " + id + " was not found")))
                .flatMap(user -> userRep.deleteById(id));
    }

    public Mono<User> findById(Long id) {
        return userRep.findById(id);
    }

    public Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    if (auth == null) {
                        return Mono.error(new BadCredentialsException("Unauthenticated"));
                    } else {
                        return findUserByUsername(auth.getName());
                    }
                });
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRep.findByUsername(username)
                .map(user -> user);
    }

    public Mono<User> findUserByUsername(String username) {
        return userRep.findByUsername(username);
    }
}