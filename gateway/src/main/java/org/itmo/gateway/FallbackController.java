package org.itmo.gateway;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {
    @RequestMapping("/service-unavailable")
    public Mono<ResponseEntity<String>> onServiceUnavailable(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String serviceName = (route == null) ? "null" : route.getId();
        return Mono.just(
                ResponseEntity.status(503).body("Service unavailable: " + serviceName)
        );
    }
}
