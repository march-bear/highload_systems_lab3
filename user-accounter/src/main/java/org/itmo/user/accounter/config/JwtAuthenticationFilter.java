package org.itmo.user.accounter.config;

import lombok.NonNull;
import org.itmo.user.accounter.services.JwtService;
import org.itmo.user.accounter.services.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_NAME = "Authorization";
    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        var authHeader = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (!StringUtils.hasLength(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        var jwt = authHeader.substring(BEARER_PREFIX.length());
        var username = jwtService.extractUserName(jwt);

        if (StringUtils.hasLength(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
            return userService.findByUsername(username)
                    .flatMap(user -> {
                        if (jwtService.isTokenValid(jwt, user)) {
                            var authToken = new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    user.getAuthorities()
                            );

                            return chain.filter(exchange).contextWrite(
                                    ReactiveSecurityContextHolder.withAuthentication(authToken)
                            );
                        } else {
                            return chain.filter(exchange);
                        }
                    }).then();
        }

        return chain.filter(exchange);
    }
}
