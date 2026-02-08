package org.itmo.user.accounter.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.NonNull;
import org.itmo.user.accounter.model.dto.ErrorDto;
import org.itmo.user.accounter.services.JwtService;
import org.itmo.user.accounter.services.UserService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
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

import java.nio.charset.StandardCharsets;

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

        try {
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
        } catch (ExpiredJwtException ex) {
            ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
            try {
                String body = writer.writeValueAsString(new ErrorDto("Token was expired"));
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return chain.filter(exchange);
    }
}
