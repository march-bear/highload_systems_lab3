package org.itmo.secs.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itmo.secs.model.entities.Notification;
import org.itmo.secs.model.entities.enums.EventType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class UserEventsConsumer {
    private final NotificationService notifService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-events", groupId = "notification-service-consumer")
    public void consumeMessage(String message) {
        try {
            JsonNode notification = objectMapper.readTree(message);

            switch (notification.get("type").asText()) {
                case "CREATED" -> onCreated(notification);
                case "DELETED" -> onDeleted(notification);
                case "UPDATED" -> onUpdated(notification);
                default -> onError(notification);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void onCreated(JsonNode tree) {
        long sub = tree.withObjectProperty("user_id").asLong();
        long userId = tree.withObjectProperty("data").get("id").asLong();
        String userName = tree.withObjectProperty("data").get("name").asText();
        String userRole = tree.withObjectProperty("data").get("role").asText();

        Notification notification = Notification.builder()
                .type(EventType.CREATED)
                .createdAt(LocalDateTime.now())
                .message(
                        "Created new user: id = " + userId
                                + "; name = " + userName
                                + "; role = " + userRole
                )
                .title("Created user, id " + userId)
                .userId(sub)
                .build();

        notifService.save(notification);
    }

    public void onDeleted(JsonNode tree) {
        long sub = tree.withObjectProperty("user_id").asLong();
        long userId = tree.withObjectProperty("data").get("id").asLong();
        String userName = tree.withObjectProperty("data").get("name").asText();
        String userRole = tree.withObjectProperty("data").get("role").asText();

        Notification notification = Notification.builder()
                .type(EventType.DELETED)
                .createdAt(LocalDateTime.now())
                .message(
                        "Deleted user: id = " + userId
                                + "; name = " + userName
                                + "; role = " + userRole
                )
                .title("Deleted user, id " + userId)
                .userId(sub)
                .build();

        notifService.save(notification);
    }

    public void onUpdated(JsonNode tree) {
        long sub = tree.withObjectProperty("user_id").asLong();
        long userId = tree.withObjectProperty("data").get("id").asLong();
        String oldRole = tree.withObjectProperty("data").get("role").get("old").asText();
        String newRole = tree.withObjectProperty("data").get("role").get("new").asText();

        Notification notification = Notification.builder()
                .type(EventType.DELETED)
                .createdAt(LocalDateTime.now())
                .message(
                        "Updated user role: id = " + userId
                                + "; role " + oldRole + " -> " + newRole
                )
                .title("Updated user role, id " + userId)
                .userId(sub)
                .build();

        notifService.save(notification);
    }

    public void onError(JsonNode tree) {
        log.error("Gotten message with unknown type {}", tree.get("type").asText());
    }
}
