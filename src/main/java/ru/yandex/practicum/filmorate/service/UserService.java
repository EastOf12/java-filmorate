package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserStorage userStorage;

    public void addFriend(Long userId, Long friendId) {
        //Проверяем, что такие пользователи существуют и указаны корректно
        if (userId.equals(friendId)) {
            log.warn("Пользователь с id {} хочет добавить в друзья сам себя", userId);
            throw new NotFoundException("Нельзя добавить в друзья самого себя");
        }
        if (userStorage.checkUserAvailability(userId)) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        if (userStorage.checkUserAvailability(friendId)) {
            log.warn("Нет пользователя с id {}", friendId);
            throw new NotFoundException("Нет пользователя с id " + friendId);
        }

        //Проверяем, что пользователь не был добавлен в друзья ранее
        Set<Long> friends = userStorage.getUser(userId).getFriends();
        if (friends.contains(friendId)) {
            log.warn("Пользователь {} уже в друзьях пользователя {}", friendId, userId);
            throw new NotFoundException("Пользователи уже дружат" + friendId);
        }

        //Добавлям пользователей в друзья
        friends.add(friendId);
        userStorage.getUser(friendId).getFriends().add(userId);

        log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
    } //Добавить пользотвателя в друзья

    public void removeFriend(Long userId, Long friendId) {
        //Проверяем, что такие пользователи существуют
        if (userStorage.checkUserAvailability(userId)) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        if (userStorage.checkUserAvailability(friendId)) {
            log.warn("Нет пользователя с id {}", friendId);
            throw new NotFoundException("Нет пользователя с id " + friendId);
        }

        //Проверяем, что пользователи дружат
        Set<Long> friends = userStorage.getUser(userId).getFriends();
        if (friends.contains(friendId) || userStorage.getUser(friendId).getFriends().contains(userId)) {
            friends.remove(friendId);
            userStorage.getUser(friendId).getFriends().remove(userId);
            log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
        } else {
            log.warn("Пользователи {} и {} не дружат ", friendId, userId);
        }
    } //Удалить пользователя из друзей

    public Collection<User> getAllUserFriends(Long userId) {
        //Проверяем, что такой пользователь существует
        if (userStorage.checkUserAvailability(userId)) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        Set<Long> friendsId = userStorage.getUser(userId).getFriends();
        if (friendsId.isEmpty()) {
            log.info("У пользователя {} нет друзей", userId);
            return new ArrayList<>();
        } else {
            List<User> allUserFriends = new ArrayList<>();
            for (Long friendId : friendsId) {
                allUserFriends.add(userStorage.getUser(friendId));
            }

            log.info("Все друзья пользователя {}", userId);
            return allUserFriends;
        }
    } //Получить всех друзей пользователя

    public Collection<User> getFriendsCommon(Long userId, Long otherUserId) {
        //Проверяем, что такие пользователи существуют.
        if (userStorage.checkUserAvailability(userId)) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        if (userStorage.checkUserAvailability(otherUserId)) {
            log.warn("Нет пользователя с id {}", otherUserId);
            throw new NotFoundException("Нет пользователя с id " + otherUserId);
        }

        //Получаем id друзей пользователей
        Set<Long> friendIdUser = userStorage.getUser(userId).getFriends();
        Set<Long> friendIdOtherUser = userStorage.getUser(otherUserId).getFriends();

        //Получаем общих пользователей.
        List<User> mutualFriends = new ArrayList<>();

        for (Long idUser : friendIdUser) {
            if (friendIdOtherUser.contains(idUser)) {
                mutualFriends.add(userStorage.getUser(idUser));
            }
        }

        return mutualFriends;
    } //Возвращает общих друзей пользотвателей.
}
