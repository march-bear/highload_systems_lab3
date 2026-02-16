package org.itmo.secs.infrastructure.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itmo.secs.domain.model.entities.Menu;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuEventProducer {

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

    public void sendMenuCreated(Menu menu) {
        var event = objectMapper.createObjectNode();

        event.put("type", "CREATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", menu.getId());
        event.withObjectProperty("data").put("date", objectToString(menu.getDate()));
        event.withObjectProperty("data").put("meal", menu.getMeal().name());
        event.withObjectProperty("data").put("user_id", menu.getUserId().toString());

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("menu-events", message);
        }
    }

    public void sendMenuDeleted(Menu menu) {
        var event = objectMapper.createObjectNode();

        event.put("type", "DELETED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", menu.getId());
        event.withObjectProperty("data").put("date", objectToString(menu.getDate()));
        event.withObjectProperty("data").put("meal", menu.getMeal().name());
        event.withObjectProperty("data").put("user_id", menu.getUserId().toString());

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("menu-events", message);
        }
    }

    public void sendMenuUpdated(Menu oldMenu, Menu newMenu) {
        var event = objectMapper.createObjectNode();

        event.put("type", "UPDATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", oldMenu.getId());

        event.withObjectProperty("data").set("date", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("date").put("old", objectToString(oldMenu.getDate()));
        event.withObjectProperty("data").withObjectProperty("date").put("new", objectToString(newMenu.getDate()));

        event.withObjectProperty("data").set("meal", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("meal").put("old", objectToString(oldMenu.getMeal()));
        event.withObjectProperty("data").withObjectProperty("meal").put("new", objectToString(newMenu.getMeal()));

        event.withObjectProperty("data").set("user_id", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("user_id").put("old", objectToString(oldMenu.getUserId()));
        event.withObjectProperty("data").withObjectProperty("user_id").put("new", objectToString(newMenu.getUserId()));

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("menu-events", message);
        }
    }

    public void sendMenuUpdatedDish(Menu oldMenu, Long dishId, boolean deleted) {
        var event = objectMapper.createObjectNode();

        event.put("type", "UPDATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", oldMenu.getId());

        event.withObjectProperty("data").set("date", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("date").put("old", objectToString(oldMenu.getDate()));
        event.withObjectProperty("data").withObjectProperty("date").put("new", objectToString(oldMenu.getDate()));

        event.withObjectProperty("data").set("meal", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("meal").put("old", objectToString(oldMenu.getMeal()));
        event.withObjectProperty("data").withObjectProperty("meal").put("new", objectToString(oldMenu.getMeal()));

        event.withObjectProperty("data").set("user_id", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("user_id").put("old", objectToString(oldMenu.getUserId()));
        event.withObjectProperty("data").withObjectProperty("user_id").put("new", objectToString(oldMenu.getUserId()));

        if (deleted) {
            event.withObjectProperty("data").put("deleted_dish", dishId);
        } else {
            event.withObjectProperty("data").put("inserted_dish", dishId);
        }

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("menu-events", message);
        }
    }
}