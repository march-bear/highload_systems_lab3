package org.itmo.secs.infrastructure.convertors;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.dto.InfoDto;
import org.itmo.secs.model.entities.Notification;
import org.itmo.secs.model.entities.enums.EventType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class InfoDtoToNotificationConverter implements Converter<InfoDto, Notification> {
    @Override
    public Notification convert(InfoDto source) {
        return Notification.builder()
                .title(source.title())
                .message(source.message())
                .createdAt(LocalDateTime.now())
                .type(EventType.INFO)
                .build();
    }
}
