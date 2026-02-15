package org.itmo.secs.notification;

import lombok.RequiredArgsConstructor;
import org.itmo.secs.model.events.MenuCreateEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMenuCreated(MenuCreateEvent event) {
        kafkaTemplate.send("menu.created", event);
    }
}