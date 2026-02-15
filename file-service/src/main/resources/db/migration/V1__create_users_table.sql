-- Создание таблицы menus
CREATE TABLE IF NOT EXISTS menus (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    user_id BIGINT NOT NULL,
    meal VARCHAR(50) NOT NULL
);

-- Создание уникального ограничения для таблицы menus
ALTER TABLE menus
ADD CONSTRAINT uc_menus_date_meal_user
UNIQUE (date, meal, user_id);

-- Создание таблицы menu_dishes
CREATE TABLE IF NOT EXISTS menu_dishes (
    id BIGSERIAL PRIMARY KEY,
    menu_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL
);

-- Создание уникального ограничения для таблицы menu_dishes
ALTER TABLE menu_dishes
ADD CONSTRAINT unique_menu_dishes
UNIQUE (menu_id, dish_id);

-- Создание внешнего ключа для таблицы menu_dishes
ALTER TABLE menu_dishes
ADD CONSTRAINT fk_menu_dishes_menu
FOREIGN KEY (menu_id)
REFERENCES menus(id)
ON DELETE CASCADE;