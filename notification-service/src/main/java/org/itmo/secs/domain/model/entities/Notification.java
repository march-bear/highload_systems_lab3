package org.itmo.secs.domain.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.itmo.secs.domain.model.entities.enums.EventType;

import java.time.LocalDateTime;

@Builder
@Data
@Entity
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
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
