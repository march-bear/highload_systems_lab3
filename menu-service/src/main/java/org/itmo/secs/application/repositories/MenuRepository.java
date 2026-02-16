package org.itmo.secs.application.repositories;

import org.itmo.secs.domain.model.entities.Menu;
import org.itmo.secs.domain.model.entities.enums.Meal;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface MenuRepository extends R2dbcRepository<Menu, Long> {
    Mono<Menu> findByMealAndDateAndUserId(Meal meal, LocalDate date, Long userId);
    Flux<Menu> findAllByUserId(Long userId);
}
