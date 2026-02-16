package org.itmo.secs.services;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.entities.Notification;
import org.itmo.secs.model.entities.enums.EventType;
import org.itmo.secs.repositories.NotificationRepository;
import org.itmo.secs.utils.exceptions.ItemNotFoundException;
import org.itmo.secs.utils.exceptions.ReExecutionException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class NotificationService {
    private final NotificationRepository notifRep;
    private final SubscriptionService subscrService;

    public Mono<Notification> save(Notification n) {
        return Mono.just(notifRep.save(n));
    }

    public Mono<Void> saveForSubscribers(Notification n) {
        subscrService.getSubscribersId().forEach(s -> {
                    n.setUserId(s);
                    notifRep.save(n);
                }
        );

        return Mono.empty();
    }

    public Mono<Notification> findForUser(
            Long userId,
            Long notifId
    ) {
        Notification notif = notifRep.findById(notifId).orElse(null);

        if (notif == null || !Objects.equals(notif.getUserId(), userId)) {
            return Mono.error(
                    new ItemNotFoundException("Notification with id " + notifId + " for user with id " + userId + " was not found")
            );
        }

        return Mono.just(notif);
    }

    public Flux<Notification> findAllForUser(
        Long userId,
        int pageNum,
        int pageSize,
        boolean isUnreadOnly
    ) {
        if (isUnreadOnly) {
            return Flux.fromIterable(notifRep.findAllUnreadForUser(userId, PageRequest.of(pageNum, pageSize)));
        }
        return Flux.fromIterable(notifRep.findAllByUserId(userId, PageRequest.of(pageNum, pageSize)));
    }

    public Flux<Notification> findAllByTypeForUser(
        Long userId,
        int pageNum,
        int pageSize,
        EventType type,
        boolean unread
    ) {
        if (unread) {
            return Flux.fromIterable(
                    notifRep.findAllUnreadByTypeForUser(userId, type.name(), PageRequest.of(pageNum, pageSize))
            );
        }
        return Flux.fromIterable(notifRep.findAllByTypeForUser(userId, type.name(), PageRequest.of(pageNum, pageSize)));
    }

    @Transactional
    public Mono<Void> setAllIsReadForUser(
        Long userId
    ) {
        List<Notification> notifs = notifRep.findAllUnreadForUser(userId);

        if (notifs.isEmpty()) {
            return Mono.error(
                    new ItemNotFoundException("Unread notifications for user with id " + userId + " were not found")
            );
        }

        notifs.forEach(
                notification -> {
                    notification.setIsRead(true);
                    notification.setReadAt(LocalDateTime.now());
                }
        );

        notifRep.saveAll(notifs);

        return Mono.empty();
    }

    @Transactional
    public Mono<Void> setIsReadForUser(
        Long userId,
        Long notifId
    ) {
        Notification notif = notifRep.findById(notifId).orElse(null);

        if (notif == null || !Objects.equals(notif.getUserId(), userId)) {
            return Mono.error(
                    new ItemNotFoundException("Notification with id " + notifId + " for user with id " + userId + " was not found")
            );
        }

        if (notif.getIsRead()) {
            return Mono.error(
                    new ReExecutionException("Notification with id " + notifId + " already was read")
            );
        }

        notif.setIsRead(true);
        notif.setReadAt(LocalDateTime.now());

        notifRep.save(notif);

        return Mono.empty();
    }
}
