package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Set;

public interface UserStorage {
    User create(User user);

    User update(User updateUser);

    Collection<User> getAll();

    User getUser(Long userId);

    void addFriend(Long userId, Long friendId);

    Collection<User> getAllUserFriends(Long userId);

    void removeFriend(Long userId, Long friendId);

    Collection<User> getUsersByIds(Set<Long> userIds);
}
