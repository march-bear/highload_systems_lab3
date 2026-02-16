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
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class DishEventsConsumer {
    private final NotificationService notifService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "dish-events", groupId = "notification-service-consumer")
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
        long dishId = tree.withObjectProperty("data").get("id").asLong();
        String dishName = tree.withObjectProperty("data").get("name").asText();

        Notification notification = Notification.builder()
                .type(EventType.CREATED)
                .createdAt(LocalDateTime.now())
                .message("Created new dish: id = " + dishId + "; name = " + dishName)
                .title("Created dish, id " + dishId)
                .build();

        notifService.saveForSubscribers(notification);
    }

    public void onDeleted(JsonNode tree) {
        long dishId = Long.parseLong(tree.withObjectProperty("data").get("id").asText());

        Notification notification = Notification.builder()
                .type(EventType.DELETED)
                .createdAt(LocalDateTime.now())
                .message("Deleted dish: id = " + dishId)
                .title("Deleted dish, id " + dishId)
                .build();

        notifService.saveForSubscribers(notification);
    }

    public void onUpdated(JsonNode tree) {
        long dishId = Long.parseLong(tree.withObjectProperty("data").get("id").asText());

        String oldName = null;
        String newName = null;

        if (tree.withObjectProperty("data").withObjectProperty("name") != null) {
            oldName = tree.withObjectProperty("data").withObjectProperty("name").get("old").asText();
            newName = tree.withObjectProperty("data").withObjectProperty("name").get("new").asText();
        }

        Map<Long, Long> itemsChanges = new HashMap<>();

        if (tree.withObjectProperty("data").withArrayProperty("items") != null) {
            tree.withObjectProperty("data").withArrayProperty("items").forEach(
                    item -> {
                        itemsChanges.put(
                            item.withObjectProperty("id").asLong(),
                            item.withObjectProperty("count").asLong()
                        );
                    }
            );
        }

        StringBuilder message = new StringBuilder("Updated dish, id = " + dishId + ".");
        if (oldName != null) {
            message.append(" name: ").append(oldName).append(" -> ").append(newName);
        }

        if (!itemsChanges.isEmpty()) {
            message.append("; items: ");
            for (Map.Entry<Long, Long> entry : itemsChanges.entrySet()) {
                message.append(entry.getKey()).append(" - ").append(entry.getValue()).append("g;");
            }
            message.deleteCharAt(message.length() - 1);
        }

        message.append(".");

        Notification notification = Notification.builder()
                .type(EventType.UPDATED)
                .createdAt(LocalDateTime.now())
                .message(message.toString())
                .title("Updated dish, id " + dishId)
                .build();

        notifService.saveForSubscribers(notification);
    }

    public void onError(JsonNode tree) {
        log.error("Gotten message with unknown type {}", tree.get("type").asText());
    }
}
