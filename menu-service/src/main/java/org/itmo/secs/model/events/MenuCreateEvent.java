package org.itmo.secs.model.events;

import org.itmo.secs.model.entities.enums.Meal;

import java.time.LocalDate;

public record  MenuCreateEvent (
    Long id,
    LocalDate date,
    Long userId,
    Meal meal
) {}
