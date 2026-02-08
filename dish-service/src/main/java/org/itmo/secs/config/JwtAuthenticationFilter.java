package org.itmo.secs.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.NonNull;
import org.itmo.secs.model.dto.ErrorDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
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
import java.nio.charset.StandardCharsets;
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
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        var authHeader = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (!StringUtils.hasLength(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        try {
            var jwt = authHeader.substring(BEARER_PREFIX.length());
            var username = extractClaim(jwt, Claims::getSubject);
            String role = extractClaim(jwt, claims -> claims.get("role")).toString();

            if (StringUtils.hasLength(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    if (isTokenValid(jwt)) {
                        return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        List.of(new SimpleGrantedAuthority(role))
                                )
                        ));
                    }
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

    public boolean isTokenValid(String token) {
        try {
            Long.parseLong(extractClaim(token, claims -> claims.get("id")).toString());
        } catch (NumberFormatException e) {
            return false;
        }

        return !extractClaim(token, Claims::getExpiration).before(new Date(System.currentTimeMillis()))
                && extractClaim(token, claims -> claims.get("role")) != null;
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
