package org.itmo.secs.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.itmo.secs.model.entities.enums.EventType;

import java.time.LocalDateTime;

@Builder
@Data
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id")
    Long userId;

    @Enumerated(EnumType.STRING)
    EventType type;

    @NotNull
    @Size(min = 3)
    String title;

    @NotNull
    @Size(min = 3)
    String message;

    @NotNull
    @Column(name = "is_read", nullable = false)
    Boolean isRead;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "read_at", nullable = false)
    LocalDateTime readAt;
}
