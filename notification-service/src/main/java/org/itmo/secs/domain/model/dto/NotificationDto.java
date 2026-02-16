package org.itmo.secs.domain.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.itmo.secs.infrastructure.json.LocalDateTimeSerializer;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String type,
        String title,
        String message,
        Boolean isRead,
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime createdAt,
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime readAt
) { }
