package org.itmo.secs.infrastructure.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itmo.secs.domain.model.entities.Item;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public String objectToString(Object node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать уведомление");
        }

        return null;
    }

    public void sendItemCreated(Item item) {
        var event = objectMapper.createObjectNode();

        event.put("type", "CREATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("name", item.getName());
        event.withObjectProperty("data").put("calories", item.getCalories());
        event.withObjectProperty("data").put("carbs", item.getCarbs());
        event.withObjectProperty("data").put("protein", item.getProtein());
        event.withObjectProperty("data").put("fats", item.getFats());

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("item-events", message);
        }
    }

    public void sendItemDeleted(Item item) {
        var event = objectMapper.createObjectNode();

        event.put("type", "DELETED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("name", item.getName());
        event.withObjectProperty("data").put("calories", item.getCalories());
        event.withObjectProperty("data").put("carbs", item.getCarbs());
        event.withObjectProperty("data").put("protein", item.getProtein());
        event.withObjectProperty("data").put("fats", item.getFats());

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("item-events", message);
        }
    }

    public void sendItemUpdated(Item oldItem, Item newItem) {
        var event = objectMapper.createObjectNode();

        event.put("type", "UPDATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", oldItem.getId());

        event.withObjectProperty("data").set("name", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("date").put("old", objectToString(oldItem.getName()));
        event.withObjectProperty("data").withObjectProperty("date").put("new", objectToString(newItem.getName()));

        event.withObjectProperty("data").set("calories", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("calories").put("old", objectToString(oldItem.getCalories()));
        event.withObjectProperty("data").withObjectProperty("calories").put("new", objectToString(newItem.getCalories()));

        event.withObjectProperty("data").set("protein", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("protein").put("old", objectToString(oldItem.getProtein()));
        event.withObjectProperty("data").withObjectProperty("protein").put("new", objectToString(newItem.getProtein()));

        event.withObjectProperty("data").set("carbs", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("carbs").put("old", objectToString(oldItem.getCarbs()));
        event.withObjectProperty("data").withObjectProperty("carbs").put("new", objectToString(newItem.getCarbs()));

        event.withObjectProperty("data").set("fats", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("fats").put("old", objectToString(oldItem.getFats()));
        event.withObjectProperty("data").withObjectProperty("fats").put("new", objectToString(newItem.getFats()));

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("item-events", message);
        }
    }
}