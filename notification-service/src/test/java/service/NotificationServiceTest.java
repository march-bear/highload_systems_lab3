package service;

import org.itmo.secs.domain.model.entities.Notification;
import org.itmo.secs.domain.model.entities.enums.EventType;
import org.itmo.secs.application.repositories.NotificationRepository;
import org.itmo.secs.application.services.NotificationService;
import org.itmo.secs.application.services.SubscriptionService;
import org.itmo.secs.exception.ItemNotFoundException;
import org.itmo.secs.exception.ReExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notifRep;

    @Mock
    private SubscriptionService subscrService;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @Captor
    private ArgumentCaptor<List<Notification>> notificationListCaptor;

    private Notification testNotification;
    private final Long userId = 1L;
    private final Long notificationId = 100L;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type(EventType.CREATED)
                .title("Test Notification")
                .message("Test message")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void save_ShouldSaveNotification() {
        when(notifRep.save(any(Notification.class))).thenReturn(testNotification);

        StepVerifier.create(notificationService.save(testNotification))
                .expectNext(testNotification)
                .verifyComplete();

        verify(notifRep).save(testNotification);
    }

//    @Test
//    void saveForSubscribers_ShouldSaveNotificationForEachSubscriber() {
//        List<Long> subscribers = List.of(1L, 2L, 3L);
//        when(subscrService.getSubscribersId()).thenReturn(subscribers);
//        when(notifRep.save(any(Notification.class))).thenReturn(testNotification);
//
//        StepVerifier.create(notificationService.saveForSubscribers(testNotification))
//                .verifyComplete();
//
//        verify(subscrService).getSubscribersId();
//        verify(notifRep, times(3)).save(notificationCaptor.capture());
//
//        List<Notification> capturedNotifications = notificationCaptor.getAllValues();
//        assertThat(capturedNotifications).hasSize(3);
//        assertThat(capturedNotifications.get(0).getUserId()).isEqualTo(1L);
//        assertThat(capturedNotifications.get(1).getUserId()).isEqualTo(2L);
//        assertThat(capturedNotifications.get(2).getUserId()).isEqualTo(3L);
//    }

    @Test
    void findForUser_WhenNotificationExistsAndBelongsToUser_ShouldReturnNotification() {
        when(notifRep.findById(notificationId)).thenReturn(Optional.of(testNotification));

        StepVerifier.create(notificationService.findForUser(userId, notificationId))
                .expectNext(testNotification)
                .verifyComplete();

        verify(notifRep).findById(notificationId);
    }

    @Test
    void findForUser_WhenNotificationDoesNotExist_ShouldReturnError() {
        when(notifRep.findById(notificationId)).thenReturn(Optional.empty());

        StepVerifier.create(notificationService.findForUser(userId, notificationId))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(notifRep).findById(notificationId);
    }

    @Test
    void findForUser_WhenNotificationBelongsToDifferentUser_ShouldReturnError() {
        when(notifRep.findById(notificationId)).thenReturn(Optional.of(testNotification));

        StepVerifier.create(notificationService.findForUser(999L, notificationId))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(notifRep).findById(notificationId);
    }

    @Test
    void findAllForUser_WithUnreadOnly_ShouldReturnUnreadNotifications() {
        List<Notification> notifications = List.of(testNotification);
        when(notifRep.findAllUnreadForUser(eq(userId), any(PageRequest.class)))
                .thenReturn(notifications);

        StepVerifier.create(notificationService.findAllForUser(userId, 0, 10, true))
                .expectNext(testNotification)
                .verifyComplete();

        verify(notifRep).findAllUnreadForUser(userId, PageRequest.of(0, 10));
        verify(notifRep, never()).findAllByUserId(anyLong(), any());
    }

    @Test
    void findAllForUser_WithAllNotifications_ShouldReturnAllNotifications() {
        List<Notification> notifications = List.of(testNotification);
        when(notifRep.findAllByUserId(eq(userId), any(PageRequest.class)))
                .thenReturn(notifications);

        StepVerifier.create(notificationService.findAllForUser(userId, 0, 10, false))
                .expectNext(testNotification)
                .verifyComplete();

        verify(notifRep).findAllByUserId(userId, PageRequest.of(0, 10));
        verify(notifRep, never()).findAllUnreadForUser(anyLong(), any());
    }

    @Test
    void findAllByTypeForUser_WithUnreadOnly_ShouldReturnUnreadNotificationsByType() {
        List<Notification> notifications = List.of(testNotification);
        when(notifRep.findAllUnreadByTypeForUser(eq(userId), eq("CREATED"), any(PageRequest.class)))
                .thenReturn(notifications);

        StepVerifier.create(notificationService.findAllByTypeForUser(userId, 0, 10, EventType.CREATED, true))
                .expectNext(testNotification)
                .verifyComplete();

        verify(notifRep).findAllUnreadByTypeForUser(userId, "CREATED", PageRequest.of(0, 10));
    }

    @Test
    void findAllByTypeForUser_WithAllNotifications_ShouldReturnAllNotificationsByType() {
        List<Notification> notifications = List.of(testNotification);
        when(notifRep.findAllByTypeForUser(eq(userId), eq("CREATED"), any(PageRequest.class)))
                .thenReturn(notifications);

        StepVerifier.create(notificationService.findAllByTypeForUser(userId, 0, 10, EventType.CREATED, false))
                .expectNext(testNotification)
                .verifyComplete();

        verify(notifRep).findAllByTypeForUser(userId, "CREATED", PageRequest.of(0, 10));
    }

    @Test
    void setAllIsReadForUser_WhenUnreadNotificationsExist_ShouldMarkAllAsRead() {
        Notification unread1 = testNotification;
        Notification unread2 = Notification.builder()
                .id(101L)
                .userId(userId)
                .isRead(false)
                .build();
        List<Notification> unreadNotifications = List.of(unread1, unread2);

        when(notifRep.findAllUnreadForUser(userId)).thenReturn(unreadNotifications);
        when(notifRep.saveAll(anyList())).thenReturn(unreadNotifications);

        StepVerifier.create(notificationService.setAllIsReadForUser(userId))
                .verifyComplete();

        verify(notifRep).findAllUnreadForUser(userId);
        verify(notifRep).saveAll(notificationListCaptor.capture());

        List<Notification> savedNotifications = notificationListCaptor.getValue();
        assertThat(savedNotifications).hasSize(2);
        assertThat(savedNotifications.get(0).getIsRead()).isTrue();
        assertThat(savedNotifications.get(0).getReadAt()).isNotNull();
        assertThat(savedNotifications.get(1).getIsRead()).isTrue();
        assertThat(savedNotifications.get(1).getReadAt()).isNotNull();
    }

    @Test
    void setAllIsReadForUser_WhenNoUnreadNotifications_ShouldReturnError() {
        when(notifRep.findAllUnreadForUser(userId)).thenReturn(List.of());

        StepVerifier.create(notificationService.setAllIsReadForUser(userId))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(notifRep).findAllUnreadForUser(userId);
        verify(notifRep, never()).saveAll(anyList());
    }

    @Test
    void setIsReadForUser_WhenNotificationExistsAndUnread_ShouldMarkAsRead() {
        when(notifRep.findById(notificationId)).thenReturn(Optional.of(testNotification));
        when(notifRep.save(any(Notification.class))).thenReturn(testNotification);

        StepVerifier.create(notificationService.setIsReadForUser(userId, notificationId))
                .verifyComplete();

        verify(notifRep).findById(notificationId);
        verify(notifRep).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getIsRead()).isTrue();
        assertThat(savedNotification.getReadAt()).isNotNull();
    }

    @Test
    void setIsReadForUser_WhenNotificationAlreadyRead_ShouldReturnError() {
        testNotification.setIsRead(true);
        when(notifRep.findById(notificationId)).thenReturn(Optional.of(testNotification));

        StepVerifier.create(notificationService.setIsReadForUser(userId, notificationId))
                .expectError(ReExecutionException.class)
                .verify();

        verify(notifRep).findById(notificationId);
        verify(notifRep, never()).save(any());
    }

    @Test
    void setIsReadForUser_WhenNotificationDoesNotExist_ShouldReturnError() {
        when(notifRep.findById(notificationId)).thenReturn(Optional.empty());

        StepVerifier.create(notificationService.setIsReadForUser(userId, notificationId))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(notifRep).findById(notificationId);
        verify(notifRep, never()).save(any());
    }
}