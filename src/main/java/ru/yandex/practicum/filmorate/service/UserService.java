package ru.yandex.practicum.filmorate.service;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
@Service
public class UserService {
    private final static Logger log = LoggerFactory.getLogger(FilmService.class);

    private final Map<Long, User> users = new HashMap<>();

    public User create(@RequestBody User user) {
        log.trace("Получен запрос на добавление нового пользователя");

        //Проходим валидацию полей.
        passValidation(user);
        log.debug("Валидация пройдена.");


        //Создаем пользователя в памяти приложения
        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Добавлен новый пользователь " + user.getId());
        return user;
    }

    public User update(@RequestBody User user) {
        log.trace("Получен запрос на обновление пользователя");

        //Проверяем корректность переданного ID
        if (user.getId() == null) {
            log.warn("Валидация не пройдена. Id должен быть указан");
            throw new ConditionsNotMetException("Id должен быть указан");
        }

        if (!users.containsKey(user.getId())) {
            log.warn("Валидация не пройдена. Пользователь с id = " + user.getId() + " не найден");
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }

        //Проходим валидацию полей.
        passValidation(user);
        log.debug("Валидация пройдена.");

        //Обновляем информацию по пользователю в памяти приложения
        users.put(user.getId(), user);
        log.info("Обновлен пользователь " + user.getId());
        return user;
    }

    public Collection<User> getAll() {
        log.info("Отправили информацию по все доступным пользователям.");
        return users.values();
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    private void passValidation(User user) {
        //Проверяем корректность заполнения полей.
        if (user.getEmail().isEmpty() || !user.getEmail().contains("@")) {
            log.warn("Валидация не пройдена. Некорректная почта " + user.getEmail());
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        } else if (user.getLogin() == null || user.getLogin().isEmpty() || user.getLogin().contains(" ")) {
            log.warn("Валидация не пройдена. Некорректный логин " + user.getLogin());
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        } else if (user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Валидация не пройдена. Некорректная дата рождеия " + user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        //Перезаписываем имя пользователя на его логин, если оно не было получено.
        if (user.getName() == null || user.getName().isEmpty()) {
            log.trace("Имя пользователя не было получено, перезаписали на логин.");
            user.setName(user.getLogin());
        }
    }
}
