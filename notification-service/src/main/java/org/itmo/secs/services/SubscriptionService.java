package org.itmo.secs.services;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.entities.Subscription;
import org.itmo.secs.repositories.SubscriptionRepository;
import org.itmo.secs.utils.exceptions.ReExecutionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@AllArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscrRep;

    public List<Long> getSubscribersId() {
        return subscrRep.findAll().stream().map(Subscription::getUserId).toList();
    }

    @Transactional
    public Mono<Void> subscribe(Long userId) {
        Subscription s = subscrRep.findById(userId).orElse(null);
        if (s != null) {
            return Mono.error(new ReExecutionException("User with id " + userId + " already has subscription"));
        }

        subscrRep.save(new Subscription(userId));

        return Mono.empty();
    }

    @Transactional
    public Mono<Void> unsubscribe(Long userId) {
        Subscription s = subscrRep.findById(userId).orElse(null);
        if (s == null) {
            return Mono.error(new ReExecutionException("User with id " + userId + " doesn't have subscription"));
        }

        subscrRep.deleteById(userId);

        return Mono.empty();
    }
}
