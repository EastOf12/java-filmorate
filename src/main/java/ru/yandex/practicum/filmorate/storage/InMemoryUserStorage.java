package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();

    @Override
    public User create(User user) {
        log.trace("Получен запрос на добавление нового пользователя");

        //Проходим валидацию полей.
        passValidationCreate(user);
        log.debug("Валидация пройдена.");


        //Создаем пользователя в памяти приложения
        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Добавлен новый пользователь {}", user.getId());
        return user;
    }

    @Override
    public User update(User updateUser) {
        log.trace("Получен запрос на обновление пользователя");

        //Проверяем корректность переданного ID
        if (updateUser.getId() == null) {
            log.warn("Валидация не пройдена. Id должен быть указан");
            throw new ValidationException("Id должен быть указан");
        }

        User user = users.get(updateUser.getId());

        if (user == null) {
            log.warn("Валидация не пройдена. Пользователь с id = {} не найден", updateUser.getId());
            throw new NotFoundException("Пользователь с id = " + updateUser.getId() + " не найден");
        } else {
            if (updateUser.getName() != null && !updateUser.getName().isBlank()) {
                user.setName(updateUser.getName());
            }

            if (updateUser.getEmail() != null && !updateUser.getEmail().isBlank() && updateUser.getEmail().contains("@")) {
                user.setEmail(updateUser.getEmail());
            }

            if (updateUser.getLogin() != null && !updateUser.getLogin().isBlank() && !updateUser.getLogin().contains(" ")) {
                user.setLogin(updateUser.getLogin());
            }

            if (updateUser.getBirthday() != null && updateUser.getBirthday().isAfter(LocalDate.now())) {
                user.setBirthday(updateUser.getBirthday());
            }
        }

        log.info("Обновлен пользователь {}", updateUser.getId());
        return updateUser;
    }

    @Override
    public Collection<User> getAll() {
        log.info("Предали информацию по все доступным пользователям.");
        return users.values();
    }

    @Override
    public User getUser(Long userID) {
        return users.get(userID);
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    private void passValidationCreate(User user) {
        //Проверяем корректность заполнения полей.
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Валидация не пройдена. Некорректная почта");
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        } else if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Валидация не пройдена. Некорректный логин {}", user.getLogin());
            throw new ValidationException("Логин не может быть пустым или содержать пробелы");
        } else if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Валидация не пройдена. Некорректная дата рождения {}", user.getBirthday());
            throw new ValidationException("Некорректная дата рождения");
        } else if (user.getFriends() == null) {
            log.warn("Валидация не пройдена. Список друзей в запросе {}",
                    (Object) null);
            throw new ValidationException("Список друзей в запросе null");
        }

        //Перезаписываем имя пользователя на его логин, если оно не было получено.
        if (user.getName() == null || user.getName().isBlank()) {
            log.trace("Имя пользователя не было получено, перезаписали на логин.");
            user.setName(user.getLogin());
        }
    }
}
