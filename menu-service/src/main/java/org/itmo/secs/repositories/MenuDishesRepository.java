package org.itmo.secs.repositories;

import org.itmo.secs.model.entities.MenuDishes;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MenuDishesRepository extends R2dbcRepository<MenuDishes, Long> {
    Flux<MenuDishes> findAllByMenuId(long menuId);
    Mono<MenuDishes> findByMenuIdAndDishId(long menuId, long dishId);
}
