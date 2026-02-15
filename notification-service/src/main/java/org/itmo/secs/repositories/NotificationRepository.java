package org.itmo.secs.repositories;

import org.itmo.secs.model.entities.Notification;
import org.itmo.secs.model.entities.enums.EventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUserId(Long userId, Pageable pageable);

    @Query("SELECT n FROM notifications n WHERE n.user_id = :userId and p.is_read = false")
    List<Notification> findAllUnreadForUser(Long userId);

    @Query("SELECT n FROM notifications n WHERE n.user_id = :userId and p.is_read = false")
    List<Notification> findAllUnreadForUser(Long userId, Pageable pageable);

    @Query("SELECT n FROM notifications n WHERE n.user_id = :userId and n.type = :type and p.is_read = false")
    List<Notification> findAllUnreadByTypeForUser(Long userId, EventType type, Pageable pageable);

    @Query("SELECT n FROM notifications n WHERE n.user_id = :userId and n.type = :type")
    List<Notification> findAllByTypeForUser(Long userId, EventType type, Pageable pageable);
}
