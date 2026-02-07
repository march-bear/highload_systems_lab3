package org.itmo.secs.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Component
public class JwtAuthenticationFilter implements WebFilter {
    @Value("${token.signing.key}")
    private String jwtSigningKey;

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_NAME = "Authorization";

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        exchange.getRequest().getHeaders().remove("X-User-Id");
        exchange.getRequest().getHeaders().remove("X-User-Role");

        var authHeader = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (!StringUtils.hasLength(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        var jwt = authHeader.substring(BEARER_PREFIX.length());
        var username = extractClaim(jwt, Claims::getSubject);

        if (StringUtils.hasLength(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (isTokenValid(jwt)) {
                String id = extractClaim(jwt, claims -> claims.get("id")).toString();
                String role = extractClaim(jwt, claims -> claims.get("role")).toString();

                exchange.getRequest().getHeaders().add("X-User-Id", id);
                exchange.getRequest().getHeaders().add("X-User-Role", role);

                return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        )
                ));
            }
        }

        return chain.filter(exchange);
    }

    public boolean isTokenValid(String token) {
        try {
            Long.parseLong(extractClaim(token, claims -> claims.get("role")).toString());
        } catch (NumberFormatException e) {
            return false;
        }

        return !extractClaim(token, Claims::getExpiration).before(new Date(System.currentTimeMillis()))
                && extractClaim(token, claims -> claims.get("id")) != null;
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claimsResolvers.apply(claims);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
