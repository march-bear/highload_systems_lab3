package org.itmo.secs.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class ItemEventsConsumer {
    private final NotificationService notifService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "item-events", groupId = "item-events-consumer")
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
        long itemId = tree.withObjectProperty("data").get("id").asLong();
        String itemName = tree.withObjectProperty("data").get("name").asText();
        int itemCalories = tree.withObjectProperty("data").get("calories").asInt();
        int itemCarbs = tree.withObjectProperty("data").get("carbs").asInt();
        int itemProtein = tree.withObjectProperty("data").get("protein").asInt();
        int itemFats = tree.withObjectProperty("data").get("fats").asInt();

        Notification notification = Notification.builder()
                .type(EventType.CREATED)
                .createdAt(LocalDateTime.now())
                .message(
                        "Created new item: id = " + itemId
                                + "; name = " + itemName
                                + "; calories = " + itemCalories
                                + "; carbs = " + itemCarbs
                                + "; protein = " + itemProtein
                                + "; fats = " + itemFats
                )
                .title("Created item, id " + itemId)
                .build();

        notifService.saveForSubscribers(notification);
    }

    public void onDeleted(JsonNode tree) {
        long itemId = Long.parseLong(tree.withObjectProperty("data").get("id").asText());
        String itemName = tree.withObjectProperty("data").get("name").asText();
        int itemCalories = tree.withObjectProperty("data").get("calories").asInt();
        int itemCarbs = tree.withObjectProperty("data").get("carbs").asInt();
        int itemProtein = tree.withObjectProperty("data").get("protein").asInt();
        int itemFats = tree.withObjectProperty("data").get("fats").asInt();

        Notification notification = Notification.builder()
                .type(EventType.DELETED)
                .createdAt(LocalDateTime.now())
                .message(
                        "Deleted item: id = " + itemId
                                + "; name = " + itemName
                                + "; calories = " + itemCalories
                                + "; carbs = " + itemCarbs
                                + "; protein = " + itemProtein
                                + "; fats = " + itemFats
                )
                .title("Deleted item, id " + itemId)
                .build();

        notifService.saveForSubscribers(notification);
    }

    public void onUpdated(JsonNode tree) {
        long itemId = Long.parseLong(tree.withObjectProperty("data").get("id").asText());
        ObjectNode itemName = tree.withObjectProperty("data").withObjectProperty("name");
        ObjectNode itemCalories = tree.withObjectProperty("data").withObjectProperty("calories");
        ObjectNode itemCarbs = tree.withObjectProperty("data").withObjectProperty("carbs");
        ObjectNode itemProtein = tree.withObjectProperty("data").withObjectProperty("protein");
        ObjectNode itemFats = tree.withObjectProperty("data").withObjectProperty("fats");

        Notification notification = Notification.builder()
                .type(EventType.UPDATED)
                .createdAt(LocalDateTime.now())
                .message("Updated item: id. "
                                + "name " + itemName.get("old").asText()  + " -> " + itemName.get("new").asText() + ";"
                                + "calories " + itemCalories.get("old").asInt()  + " -> " + itemCalories.get("new").asInt() + ";"
                                + "carbs " + itemCarbs.get("old").asInt()  + " -> " + itemCarbs.get("new").asInt() + ";"
                                + "protein " + itemProtein.get("old").asInt()  + " -> " + itemProtein.get("new").asInt() + ";"
                                + "fats " + itemFats.get("old").asInt()  + " -> " + itemFats.get("new").asInt() + ";"
                        )
                .title("Updated item, id " + itemId)
                .build();

        notifService.saveForSubscribers(notification);
    }

    public void onError(JsonNode tree) {
        log.error("Gotten message with unknown type {}", tree.get("type").asText());
    }
}
