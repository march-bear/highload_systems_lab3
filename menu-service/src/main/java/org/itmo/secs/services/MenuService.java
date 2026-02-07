package org.itmo.secs.services;

import lombok.AllArgsConstructor;
import org.itmo.secs.client.DishServiceClient;
import org.itmo.secs.client.UserServiceClient;
import org.itmo.secs.model.dto.DishDto;
import org.itmo.secs.model.entities.Menu;
import org.itmo.secs.model.entities.enums.Meal;
import org.itmo.secs.repositories.MenuRepository;
import org.itmo.secs.utils.exceptions.DataIntegrityViolationException;
import org.itmo.secs.utils.exceptions.ItemNotFoundException;
import org.itmo.secs.utils.exceptions.ServiceUnavailableException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Objects;

@Service
@AllArgsConstructor
public class MenuService {
    private MenuRepository menuRep;
    private MenuDishesService menuDishesService;
    private DishServiceClient dishServiceClient;
    private UserServiceClient userServiceClient;

    public Mono<Menu> save(Menu menu) {
        return menuRep.findByMealAndDateAndUserId(
            menu.getMeal(),
            menu.getDate(),
            menu.getUserId()
        )
                .doOnNext(x -> {
                    throw new DataIntegrityViolationException("Menu with given key already exists");
                })
                .switchIfEmpty(menuRep.save(menu));
    }

    public void updateForUser(Menu menu, Long userId) {
        findById(menu.getId())
                .filter(menu_ -> Objects.equals(menu_.getUserId(), userId))
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Menu with id " + menu.getId() + " was not found")))
                .flatMap(existingMenu -> findByKey(menu.getMeal(), menu.getDate(), menu.getUserId())
                    .flatMap(foundMenu -> {
                        if (!Objects.equals(foundMenu.getId(), menu.getId())) {
                            return Mono.error(new DataIntegrityViolationException("Menu with given new key already exists"));
                        } else {
                            return Mono.just(existingMenu);
                        }
                    })
                )
                .flatMap(existingMenu -> {
                    existingMenu.setMeal(menu.getMeal());
                    existingMenu.setDate(menu.getDate());
                    existingMenu.setUserId(menu.getUserId());
                    return Mono.fromCallable(() -> menuRep.save(existingMenu));
                })
                .subscribe();
    }

    public Mono<Void> deleteForUser(Long id, Long userId) {
        return menuRep.findById(id)
                .filter(menu -> Objects.equals(menu.getUserId(), userId))
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Menu with id " + id + " was not found")))
                .flatMap(x -> menuRep.deleteById(id));
    }

    public Mono<Menu> findById(Long id) {
       return menuRep.findById(id);
    }

    public Mono<Menu> findByKey(Meal meal, LocalDate date, Long userId) {
        return  menuRep.findByMealAndDateAndUserId(meal, date, userId);
    }

    public Mono<Void> includeDishToMenuForUser(Long dishId, Long menuId, Long userId) {
        return menuRep.findById(menuId)
                .filter(menu -> Objects.equals(menu.getUserId(), userId))
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Menu with id " + menuId + " was not found")))
                .flatMap(menu -> dishServiceClient.getById(dishId)
                        .onErrorResume(e -> {
                            if (e instanceof ServiceUnavailableException) {
                                return Mono.error(e);
                            } else {
                                return Mono.error(new ItemNotFoundException("Dish with id " + dishId + " was not found"));
                            }
                        })
                        .flatMap((dish) -> menuDishesService.saveByIds(menuId, dishId)))
                .then();
    }

    public void deleteDishFromMenuForUser(Long dishId, Long menuId, Long userId) {
        findById(menuId)
                .filter(menu -> Objects.equals(menu.getUserId(), userId))
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Menu with id " + menuId.toString() + " was not found")))
                .flatMap(menu -> menuDishesService.deleteByIds(menuId, dishId))
                .subscribe();
    }

    public Flux<DishDto> makeListOfDishesForUser(Long menuId, Long userId) {
        return menuRep.findById(menuId)
                .filter(menu -> Objects.equals(menu.getUserId(), userId))
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Menu with id " + menuId + " was not found")))
                .flatMapMany(x -> menuDishesService.getDishesIdByMenuId(x.getId()))
                .flatMap(dishId -> dishServiceClient.getById(dishId)
                        .onErrorResume(e -> {
                            if (e instanceof ServiceUnavailableException) {
                                return Mono.error(e);
                            } else {
                                return Mono.just(new DishDto(dishId, "(not found)", 0, 0, 0, 0));
                            }
                        }));
    }

    public Flux<Menu> findAllByUserId(int page, int size, Long userId) {
        return menuRep.findAll()
                .filter(menu -> Objects.equals(menu.getUserId(), userId))
                .skip((long) page * size)
                .limitRate(size);
    }

    public Flux<Menu> findAll(int page, int size) {
        return menuRep.findAll().skip((long) page * size).limitRate(size);
    }

    public Flux<Menu> findAllByUsername(String authHeader, String username) {
        return userServiceClient.getByName(authHeader, username)
                .onErrorResume(e -> {
                    if (e instanceof ServiceUnavailableException) {
                        return Mono.error(e);
                    } else {
                        return Mono.error(new ItemNotFoundException("User with username " + username + " was not found"));
                    }
                })
                .flatMapMany(user -> menuRep.findAllByUserId(user.id()));
    }
}
