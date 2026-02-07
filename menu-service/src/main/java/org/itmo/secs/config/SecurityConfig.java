package org.itmo.secs.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import reactor.core.publisher.Mono;

@Configuration
@AllArgsConstructor
@EnableWebFluxSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(spec -> spec.authenticationEntryPoint(
                        new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
                ))
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.GET, "/menu").authenticated()
                        .pathMatchers(HttpMethod.PUT, "/menu").authenticated()
                        .pathMatchers(HttpMethod.DELETE, "/menu").authenticated()
                        .pathMatchers(HttpMethod.POST, "/menu").hasAuthority("USER")

                        .pathMatchers("/menu/**").hasAuthority("USER")

                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                )
                .authenticationManager(authenticationManager())
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager() {
        return authentication -> {
            if (authentication.getDetails() != null && !authentication.getAuthorities().isEmpty()) {
                return Mono.just(authentication);
            } else {
                return Mono.error(new RuntimeException(authentication.getDetails() + "\n" + authentication.getAuthorities().toString()));
            }
        };
    }
}
