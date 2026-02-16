package org.itmo.secs.services;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.entities.Subscription;
import org.itmo.secs.repositories.SubscriptionRepository;
import org.itmo.secs.infrastructure.exceptions.ReExecutionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@AllArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscrRep;

    public List<Long> getSubscribersId() {
        return subscrRep.findAll().stream().map(Subscription::getUserId).toList();
    }

    public Mono<Subscription> findById(Long id) {
        return Mono.fromCallable(() -> subscrRep.findById(id).orElse(null))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<Void> subscribe(Long userId) {
        return findById(userId)
                .flatMap(x -> Mono.error(new ReExecutionException("User with id " + userId + " already has subscription")))
                .switchIfEmpty(
                        Mono.fromCallable(() -> subscrRep.save(new Subscription(userId)))
                                .subscribeOn(Schedulers.boundedElastic())
                )
                .then();
    }

    @Transactional
    public Mono<Void> unsubscribe(Long userId) {
        return findById(userId)
                .switchIfEmpty(Mono.error(new ReExecutionException("User with id " + userId + " doesn't have subscription")))
                .flatMap(subscription ->
                        Mono.fromRunnable(() -> subscrRep.delete(subscription))
                                .subscribeOn(Schedulers.boundedElastic())
                )
                .then();
    }
}
