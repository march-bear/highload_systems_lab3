package org.itmo.secs.application.services;

import lombok.AllArgsConstructor;
import org.itmo.secs.domain.model.entities.MenuDishes;
import org.itmo.secs.application.repositories.MenuDishesRepository;
import org.itmo.secs.exception.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class MenuDishesService {
    private MenuDishesRepository menuDishesRep;

    public Mono<MenuDishes> saveByIds(Long menu_id, Long dish_id) {
        return menuDishesRep.findByMenuIdAndDishId(menu_id, dish_id)
                .doOnNext(x -> {
                    throw new DataIntegrityViolationException(
                            "Dish with id " + dish_id + " already in menu with id " + menu_id
                    );
                })
                .switchIfEmpty(menuDishesRep.save(new MenuDishes(null, menu_id, dish_id)));
    }

    public Mono<Void> deleteByIds(Long menu_id, Long dish_id) {
        return menuDishesRep.findByMenuIdAndDishId(menu_id, dish_id)
                .switchIfEmpty(Mono.error(
                    new DataIntegrityViolationException(
                            "Dish with id " + dish_id + " isn't in menu with id " + menu_id
                    ))
                )
                .flatMap(x -> menuDishesRep.deleteById(x.getId()));
    }

    public Flux<Long> getDishesIdByMenuId(Long menuId) {
        return menuDishesRep.findAllByMenuId(menuId)
                .map(MenuDishes::getDishId);
    }
}
