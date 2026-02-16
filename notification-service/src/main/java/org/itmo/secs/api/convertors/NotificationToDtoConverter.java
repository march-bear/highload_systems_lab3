package org.itmo.secs.api.convertors;

import lombok.AllArgsConstructor;
import org.itmo.secs.domain.model.dto.NotificationDto;
import org.itmo.secs.domain.model.entities.Notification;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class NotificationToDtoConverter implements Converter<Notification, NotificationDto> {
    @Override
    public NotificationDto convert(Notification source) {
        return new NotificationDto(
                source.getId(),
                source.getType().name(),
                source.getTitle(),
                source.getMessage(),
                source.getIsRead(),
                source.getCreatedAt(),
                source.getReadAt()
        );
    }
}
