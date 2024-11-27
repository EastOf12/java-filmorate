package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dal.UserFriendDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;

@Service
@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {
    private final UserDbStorage userDbStorage;
    private final UserFriendDbStorage userFriendDbStorage;

    public InMemoryUserStorage(UserDbStorage userDbStorage, UserFriendDbStorage userFriendDbStorage) {
        this.userDbStorage = userDbStorage;
        this.userFriendDbStorage = userFriendDbStorage;
    }

    @Override
    public User create(User user) {
        log.trace("Получен запрос на добавление нового пользователя");

        //Проходим валидацию полей.
        passValidationCreate(user);
        log.debug("Валидация пройдена.");

        //Создаем пользователя в базе данных
        userDbStorage.createUser(user);

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

        User user = userDbStorage.findById(updateUser.getId());

        //Валидируем поля
        if (updateUser.getName() != null && !updateUser.getName().isBlank()) {
            user.setName(updateUser.getName());
        }

        if (updateUser.getEmail() != null && !updateUser.getEmail().isBlank() && updateUser.getEmail().contains("@")) {
            user.setEmail(updateUser.getEmail());
        }

        if (updateUser.getLogin() != null && !updateUser.getLogin().isBlank() && !updateUser.getLogin().contains(" ")) {
            user.setLogin(updateUser.getLogin());
        }

        if (updateUser.getBirthday() != null && updateUser.getBirthday().isBefore(LocalDate.now())) {
            user.setBirthday(updateUser.getBirthday());
        }

        //Обновляем пользователя в БД
        userDbStorage.updateUser(user);


        log.info("Обновлен пользователь {}", updateUser.getId());
        return user;
    }

    @Override
    public Collection<User> getAll() {
        log.info("Предали информацию по все доступным пользователям.");

        return userDbStorage.findAll();
    }

    @Override
    public Collection<User> getFriendsCommon(Long userId, Long otherId) {
        return userDbStorage.getFriendsCommon(userId, otherId);
    }

    @Override
    public User getUser(Long userID) {
        return userDbStorage.findById(userID);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {

        //Проверяем, что такие пользователи существуют в бд и указаны корректно
        if (userId.equals(friendId)) {
            log.warn("Пользователь с id {} хочет добавить в друзья сам себя", userId);
            throw new NotFoundException("Нельзя добавить в друзья самого себя");
        }

        User user = getUser(userId);
        User otherUser = getUser(friendId);


        if (user == null) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        if (otherUser == null) {
            log.warn("Нет пользователя с id {}", friendId);
            throw new NotFoundException("Нет пользователя с id " + friendId);
        }

        //Проверяем, что пользователь не был добавлен в друзья ранее
        if (checkFriendship(userId, friendId)) {
            log.warn("Пользователь {} уже в друзьях пользователя {}", friendId, userId);
            throw new NotFoundException("Пользователи уже дружат" + friendId);
        }

        //Добавляем пользователей в друзья в БД
        userFriendDbStorage.addFriend(userId, friendId);

        log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {

        //Проверяем, что такие пользователи существуют и являются друзьями
        User user = getUser(userId);
        User otherUser = getUser(friendId);

        if (user == null) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        if (otherUser == null) {
            log.warn("Нет пользователя с id {}", friendId);
            throw new NotFoundException("Нет пользователя с id " + friendId);
        }

        if (checkFriendship(userId, friendId)) {
            userFriendDbStorage.removeFriend(userId, friendId, friendId, userId);
            log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
        } else {
            log.warn("Пользователь {} не дружит с пользователем {}", friendId, userId);
        }
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

    private boolean checkFriendship(Long userId, Long friendId) {
        return userFriendDbStorage.checkFriendship(userId, friendId) == 1;
    } //Проверяем, являются ли пользователи друзьями.

    public Collection<User> getAllUserFriends(Long userId) {
        return userDbStorage.getAllUserFriends(userId);
    }
}
