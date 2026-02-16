package service;

import org.itmo.secs.model.entities.Subscription;
import org.itmo.secs.repositories.SubscriptionRepository;
import org.itmo.secs.services.SubscriptionService;
import org.itmo.secs.infrastructure.exceptions.ReExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscrRep;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private final Long userId = 1L;

    @Test
    void getSubscribersId_ShouldReturnListOfUserIds() {
        List<Subscription> subscriptions = List.of(
                new Subscription(1L),
                new Subscription(2L),
                new Subscription(3L)
        );
        when(subscrRep.findAll()).thenReturn(subscriptions);

        List<Long> result = subscriptionService.getSubscribersId();

        assertThat(result).hasSize(3).containsExactly(1L, 2L, 3L);
        verify(subscrRep).findAll();
    }

    @Test
    void getSubscribersId_WhenNoSubscribers_ShouldReturnEmptyList() {
        when(subscrRep.findAll()).thenReturn(List.of());

        List<Long> result = subscriptionService.getSubscribersId();

        assertThat(result).isEmpty();
        verify(subscrRep).findAll();
    }

    @Test
    void subscribe_WhenUserNotSubscribed_ShouldSubscribe() {
        when(subscrRep.findById(userId)).thenReturn(Optional.empty());

        StepVerifier.create(subscriptionService.subscribe(userId))
                .verifyComplete();

        verify(subscrRep).findById(userId);
        verify(subscrRep).save(any(Subscription.class));
    }

    @Test
    void subscribe_WhenUserAlreadySubscribed_ShouldReturnError() {
        when(subscrRep.findById(userId)).thenReturn(Optional.of(new Subscription(userId)));

        StepVerifier.create(subscriptionService.subscribe(userId))
                .expectError(ReExecutionException.class)
                .verify();

        verify(subscrRep).findById(userId);
        verify(subscrRep, never()).save(any());
    }

    @Test
    void unsubscribe_WhenUserSubscribed_ShouldUnsubscribe() {
        when(subscrRep.findById(userId)).thenReturn(Optional.of(new Subscription(userId)));

        StepVerifier.create(subscriptionService.unsubscribe(userId))
                .verifyComplete();

        verify(subscrRep).findById(userId);
        verify(subscrRep).deleteById(userId);
    }

    @Test
    void unsubscribe_WhenUserNotSubscribed_ShouldReturnError() {
        when(subscrRep.findById(userId)).thenReturn(Optional.empty());

        StepVerifier.create(subscriptionService.unsubscribe(userId))
                .expectError(ReExecutionException.class)
                .verify();

        verify(subscrRep).findById(userId);
        verify(subscrRep, never()).deleteById(anyLong());
    }
}