package org.itmo.user.accounter.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itmo.user.accounter.model.entities.User;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

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

    public void sendUserCreated(User user, Long admin) {
        var event = objectMapper.createObjectNode();

        event.put("type", "CREATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", user.getId());
        event.withObjectProperty("data").put("name", user.getUsername());
        event.withObjectProperty("data").put("role", user.getRole().name());
        event.put("user_id", admin);

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("user-events", message);
        }
    }

    public void sendUserDeleted(User user, Long admin) {
        var event = objectMapper.createObjectNode();

        event.put("type", "DELETED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", user.getId());
        event.withObjectProperty("data").put("name", user.getUsername());
        event.withObjectProperty("data").put("role", user.getRole().name());
        event.put("user_id", admin);

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("user-events", message);
        }
    }

    public void sendUserUpdated(User oldUser, User newUser, Long admin) {
        var event = objectMapper.createObjectNode();

        event.put("type", "UPDATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("id", oldUser.getId());
        event.withObjectProperty("data").set("role", objectMapper.createObjectNode());
        event.withObjectProperty("data").withObjectProperty("role").put("old", oldUser.getRole().name());
        event.withObjectProperty("data").withObjectProperty("role").put("new", newUser.getRole().name());
        event.put("user_id", admin);

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("user-events", message);
        }
    }
}