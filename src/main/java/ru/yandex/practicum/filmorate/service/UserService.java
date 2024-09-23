package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.*;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public void addFriend(Long userId, Long friendId) {
        //Проверяем, что такие пользователи существуют и указаны корректно
        if (userId.equals(friendId)) {
            log.warn("Пользователь с id {} хочет добавить в друзья сам себя", userId);
            throw new NotFoundException("Нельзя добавить в друзья самого себя");
        }

        User user = userStorage.getUser(userId);
        User otherUser = userStorage.getUser(friendId);
        if (user == null) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        if (otherUser == null) {
            log.warn("Нет пользователя с id {}", friendId);
            throw new NotFoundException("Нет пользователя с id " + friendId);
        }

        //Проверяем, что пользователь не был добавлен в друзья ранее
        Set<Long> friends = user.getFriends();
        if (friends.contains(friendId)) {
            log.warn("Пользователь {} уже в друзьях пользователя {}", friendId, userId);
            throw new NotFoundException("Пользователи уже дружат" + friendId);
        }

        //Добавлям пользователей в друзья
        friends.add(friendId);
        otherUser.getFriends().add(userId);

        log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
    } //Добавить пользотвателя в друзья

    public void removeFriend(Long userId, Long friendId) {
        //Проверяем, что такие пользователи существуют
        User user = userStorage.getUser(userId);
        User otherUser = userStorage.getUser(friendId);

        if (user == null) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        if (otherUser == null) {
            log.warn("Нет пользователя с id {}", friendId);
            throw new NotFoundException("Нет пользователя с id " + friendId);
        }

        //Проверяем, что пользователи дружат
        Set<Long> userFriends = user.getFriends();
        Set<Long> otherUserFriends = otherUser.getFriends();
        if (userFriends.contains(friendId) || otherUserFriends.contains(userId)) {
            userFriends.remove(friendId);
            otherUserFriends.remove(userId);
            log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
        } else {
            log.warn("Пользователи {} и {} не дружат ", friendId, userId);
        }
    } //Удалить пользователя из друзей

    public Collection<User> getAllUserFriends(Long userId) {
        //Проверяем, что такой пользователь существует
        User user = userStorage.getUser(userId);

        if (user == null) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        Set<Long> friendsId = user.getFriends();
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
        User user = userStorage.getUser(userId);
        User otherUser = userStorage.getUser(otherUserId);

        if (user == null) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        if (otherUser == null) {
            log.warn("Нет пользователя с id {}", otherUserId);
            throw new NotFoundException("Нет пользователя с id " + otherUserId);
        }

        //Получаем id друзей пользователей
        Set<Long> friendIdUser = user.getFriends();
        Set<Long> friendIdOtherUser = otherUser.getFriends();

        //Получаем общих пользователей.
        Set<Long> mutualFriendId = new HashSet<>(friendIdUser);
        mutualFriendId.retainAll(friendIdOtherUser);

        List<User> mutualFriends = new ArrayList<>();

        for (Long idUser : mutualFriendId) {
            mutualFriends.add(userStorage.getUser(idUser));
        }

        return mutualFriends;
    } //Возвращает общих друзей пользотвателей.
}
