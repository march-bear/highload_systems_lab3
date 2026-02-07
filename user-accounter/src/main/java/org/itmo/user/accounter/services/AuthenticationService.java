package org.itmo.user.accounter.services;

import org.itmo.user.accounter.model.dto.JwtTokenDto;
import org.itmo.user.accounter.model.dto.UserAuthDto;
import org.itmo.user.accounter.model.entities.User;
import org.itmo.user.accounter.model.entities.enums.UserRole;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class AuthenticationService {
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ReactiveAuthenticationManager authenticationManager;

    public Mono<JwtTokenDto> signUp(UserAuthDto request) {

        var user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .build();

        return userService.create(user)
                .map(jwtService::generateToken)
                .map(JwtTokenDto::new);
    }

    public Mono<JwtTokenDto> signIn(UserAuthDto request) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.username(),
                request.password()
        ))
                .flatMap(auth -> userService.findByUsername(request.username()))
                .map(user -> new JwtTokenDto(
                        jwtService.generateToken(user)
                ));
    }
}
