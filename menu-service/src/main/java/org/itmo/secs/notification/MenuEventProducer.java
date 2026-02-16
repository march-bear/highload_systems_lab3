package org.itmo.secs.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itmo.secs.model.entities.Menu;
import org.itmo.secs.utils.json.LocalDateSerializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final LocalDateSerializer localDateSerializer = new LocalDateSerializer();

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

        event.put("id", menu.getId());
        event.put("type", "CREATED");

        event.set("data", objectMapper.createObjectNode());
        event.withObjectProperty("data").put("date", objectToString(menu.getDate()));
        event.withObjectProperty("data").put("meal", menu.getMeal().name());
        event.withObjectProperty("data").put("user_id", menu.getUserId().toString());

        var message = objectToString(event);
        if (message != null) {
            kafkaTemplate.send("menu-events", message);
        }
    }
}