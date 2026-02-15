package org.itmo.secs.notification;

import org.itmo.secs.model.events.MenuCreateEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MenuEventListener {

    @KafkaListener(topics = "menu.created")
    public void handle(MenuCreateEvent event) {
        System.out.println("Menu updated: " + event);
    }
}
