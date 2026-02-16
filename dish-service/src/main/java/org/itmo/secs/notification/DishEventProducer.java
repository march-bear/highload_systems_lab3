package org.itmo.secs.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itmo.secs.model.entities.Dish;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishEventProducer {

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

    public void sendDishCreated(Dish dish) {
        var event = objectMapper.createObjectNode();

        event.put("type", "CREATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", dish.getId());
        event.withObjectProperty("data").put("name", dish.getName());

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("dish-events", message);
        }
    }

    public void sendDishDeleted(Dish dish) {
        var event = objectMapper.createObjectNode();

        event.put("type", "DELETED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", dish.getId());
        event.withObjectProperty("data").put("name", dish.getName());

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("dish-events", message);
        }
    }

    public void sendDishUpdated(Dish oldDish, Dish newDish) {
        var event = objectMapper.createObjectNode();

        event.put("type", "UPDATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", oldDish.getId());

        event.withObjectProperty("data").set("name", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("name").put("old", objectToString(oldDish.getName()));
        event.withObjectProperty("data").withObjectProperty("name").put("new", objectToString(newDish.getName()));

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("dish-events", message);
        }
    }

    public void sendDishUpdatedDish(Dish oldDish, Long itemId, Integer count) {
        var event = objectMapper.createObjectNode();

        event.put("type", "UPDATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", oldDish.getId());

        event.withObjectProperty("data").set("name", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("name").put("old", objectToString(oldDish.getName()));
        event.withObjectProperty("data").withObjectProperty("name").put("new", objectToString(oldDish.getName()));

        event.withObjectProperty("data").set("items", objectMapper.createArrayNode());
        event.withObjectProperty("data").withArrayProperty("items").add(objectMapper.createObjectNode());
        ((ObjectNode) event.withObjectProperty("data").withArrayProperty("items").get(0)).put("id", itemId);
        ((ObjectNode) event.withObjectProperty("data").withArrayProperty("items").get(0)).put("count", count);

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("dish-events", message);
        }
    }
}