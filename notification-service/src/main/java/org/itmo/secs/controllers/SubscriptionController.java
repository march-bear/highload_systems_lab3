package org.itmo.secs.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.itmo.secs.services.SubscriptionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping(value = "info")
@Tag(name = "Подписка на рассылку (Info Subscription API)")
public class SubscriptionController {
    private final SubscriptionService subscrService;

    /* подписаться на рассылку сообщений INFO */
    @PostMapping("subscribe")
    public Mono<Void> subscribe(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId
    ) {
        return subscrService.subscribe(userId);
    }

    /* отподписаться от рассылки сообщений INFO */
    @PostMapping("unsubscribe")
    public Mono<Void> unsubscribe(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId
    ) {
        return subscrService.unsubscribe(userId);
    }
}
