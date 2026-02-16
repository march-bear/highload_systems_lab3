package org.itmo.secs.api.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.itmo.secs.domain.model.dto.InfoDto;
import org.itmo.secs.domain.model.dto.NotificationDto;
import org.itmo.secs.domain.model.entities.Notification;
import org.itmo.secs.domain.model.entities.enums.EventType;
import org.itmo.secs.application.services.NotificationService;
import org.itmo.secs.config.PagingConf;
import org.itmo.secs.exception.DataIntegrityViolationException;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@AllArgsConstructor
@RestController
@RequestMapping(value = "notification")
@Tag(name = "Уведомления (Notification API)")
public class NotificationController {
    private ConversionService conversionService;
    private final NotificationService notifService;
    private final PagingConf pagingConf;

    /* рассылка всем подписчикам информационного сообщения */
    @PostMapping
    public Mono<Void> info(
            @Valid @RequestBody InfoDto dto
    ) {
        return notifService.saveForSubscribers(conversionService.convert(dto, Notification.class));
    }

    /* сделать уведомление прочитанным */
    @PutMapping
    public Mono<Void> setIsRead(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(required = true, name = "id")
            @RequestParam("id") Long notifId
    ) {
        return notifService.setIsReadForUser(userId, notifId);
    }

    /* сделать Все уведомления прочитанными */
    @PutMapping("/all")
    public Mono<Void> setIsReadAll(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId
    ) {
        return notifService.setAllIsReadForUser(userId);
    }

    /* найти уведомление по ID */
    @GetMapping
    public Mono<NotificationDto> findById(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(required = true, name = "id")
            @RequestParam("id") Long notifId
    ) {
        return notifService.findForUser(userId, notifId)
                .map(n -> Objects.requireNonNull(conversionService.convert(n, NotificationDto.class)));
    }

    /* найти все уведомления (есть пагинация) */
    @GetMapping("/all")
    public Flux<NotificationDto> findAll(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Тип уведомления (CREATED, DELETED, UPDATED, INFO)", example = "INFO")
            @RequestParam(name="type", required=false) String _type,
            @Parameter(description = "Флаг, при установке выводятся только непрочитанные", example = "true")
            @RequestParam(name="unread-only", required=true) Boolean unread,
            @Parameter(description = "Номер страницы (нумерация с 0)", example = "0")
            @RequestParam(name="page-number", required=false) Integer _pageNumber,
            @Parameter(description = "Размер страницы (по умолчанию 50)", example = "10")
            @RequestParam(name="page-size", required=false) Integer _pageSize
    ) {
        int pageNumber = (_pageNumber == null) ? 0 : _pageNumber;
        int pageSize = (_pageSize == null)
                ? pagingConf.getDefaultPageSize()
                : (_pageSize > pagingConf.getMaxPageSize())
                ? pagingConf.getMaxPageSize()
                : _pageSize;

        if (_type == null) {
            return notifService.findAllForUser(userId, pageNumber, pageSize, unread)
                    .map(n -> Objects.requireNonNull(conversionService.convert(n, NotificationDto.class)));
        }

        EventType type;

        try {
            type = EventType.valueOf(_type);
        } catch (IllegalArgumentException ex) {
            return Flux.error(new DataIntegrityViolationException("Type can be CREATED, DELETED, UPDATED or INFO"));
        }

        return notifService.findAllByTypeForUser(userId, pageNumber, pageSize, type, unread)
                .map(n -> Objects.requireNonNull(conversionService.convert(n, NotificationDto.class)));
    }
}
