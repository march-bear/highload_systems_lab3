package org.itmo.secs.application.repositories;

import org.itmo.secs.domain.model.entities.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUserId(Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId and n.isRead = false")
    List<Notification> findAllUnreadForUser(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId and n.isRead = false")
    List<Notification> findAllUnreadForUser(Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId and n.type = :type and n.isRead = false")
    List<Notification> findAllUnreadByTypeForUser(Long userId, String type, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId and n.type = :type")
    List<Notification> findAllByTypeForUser(Long userId, String type, Pageable pageable);
}
