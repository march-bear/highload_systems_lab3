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
public class MenuEventsConsumer {
    private final NotificationService notifService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "menu-events", groupId = "notification-service-consumer")
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
        long menuId = tree.get("id").asLong();
        String menuDate = tree.withObjectProperty("data").get("date").asText();
        String menuMeal = tree.withObjectProperty("data").get("meal").asText();
        long menuUserId = tree.withObjectProperty("data").get("user_id").asLong();

        Notification notification = Notification.builder()
                .type(EventType.CREATED)
                .createdAt(LocalDateTime.now())
                .message("Created new menu: id = " + menuId + "; meal = " + menuMeal + "; date = " + menuDate + ".")
                .title("Created menu, id " + menuId)
                .userId(menuUserId)
                .isRead(false)
                .build();

        notifService.save(notification);
    }

    public void onDeleted(JsonNode tree) {
        long menuId = tree.withObjectProperty("data").get("id").asLong();
        String menuDate = tree.withObjectProperty("data").get("date").asText();
        String menuMeal = tree.withObjectProperty("data").get("meal").asText();
        long menuUserId = tree.withObjectProperty("data").get("user_id").asLong();

        Notification notification = Notification.builder()
                .type(EventType.DELETED)
                .createdAt(LocalDateTime.now())
                .message("Deleted menu: id = " + menuId + "; meal = " + menuMeal + "; date = " + menuDate + ".")
                .title("Deleted menu, id " + menuId)
                .userId(menuUserId)
                .build();

        notifService.save(notification);
    }

    public void onUpdated(JsonNode tree) {
        long menuId = tree.withObjectProperty("data").get("id").asLong();
        long menuUserId = tree.withObjectProperty("data").get("user_id").asLong();

        String oldDate = null;
        String newDate = null;

        if (tree.withObjectProperty("data").withObjectProperty("date") != null) {
            oldDate = tree.withObjectProperty("data").withObjectProperty("date").get("old").asText();
            newDate = tree.withObjectProperty("data").withObjectProperty("date").get("new").asText();
        }

        String oldMeal = null;
        String newMeal = null;

        if (tree.withObjectProperty("data").withObjectProperty("meal") != null) {
            oldMeal = tree.withObjectProperty("data").withObjectProperty("meal").get("old").asText();
            newMeal = tree.withObjectProperty("data").withObjectProperty("meal").get("new").asText();
        }

        Long deleted = null;
        if (tree.withObjectProperty("data").get("deleted_dish") != null) {
            deleted = tree.withObjectProperty("data").get("deleted_dish").asLong();
        }

        Long inserted = null;
        if (tree.withObjectProperty("data").get("inserted_dish") != null) {
            inserted = tree.withObjectProperty("data").get("inserted_dish").asLong();
        }

        StringBuilder message = new StringBuilder("Updated menu, id = " + menuId);
        if (oldDate != null) {
            message.append("; date: ").append(oldDate).append(" -> ").append(newDate);
        }

        if (oldMeal != null) {
            message.append("; meal: ").append(oldMeal).append(" -> ").append(newMeal);
        }

        if (deleted != null) {
            message.append("; dish ").append(deleted).append(" - deleted.");
        }

        if (inserted != null) {
            message.append("; dish ").append(inserted).append(" - inserted.");
        }

        Notification notification = Notification.builder()
                .type(EventType.UPDATED)
                .createdAt(LocalDateTime.now())
                .message(message.toString())
                .title("Updated menu, id " + menuId)
                .userId(menuUserId)
                .build();

        notifService.save(notification);
    }

    public void onError(JsonNode tree) {
        log.error("Gotten message with unknown type {}", tree.get("type").asText());
    }
}
