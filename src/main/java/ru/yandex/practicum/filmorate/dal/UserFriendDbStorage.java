package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Slf4j
@Repository
public class UserFriendDbStorage extends BaseDbStorage<User> {

    private static final String CHECK_FRIENDSHIP_QUERY = """
            SELECT COUNT(*) AS friend_exists
            FROM user_friends
            WHERE user_id = ? AND friend_id = ?;""";

    private static final String ADD_FRIEND_QUERY = "INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)";

    private static final String GET_USER_FRIENDS_ID_QUERY = """
            SELECT friend_id
            FROM user_friends
            WHERE user_id = ?;""";

    private static final String REMOVE_FRIEND_QUERY = "DELETE FROM user_friends\n" +
            "WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";

    public UserFriendDbStorage(JdbcTemplate jdbcTemplate, UserRowMapper mapper) {
        super(jdbcTemplate, mapper, User.class);
    }

    public Integer checkFriendship(Object... params) {
        return jdbcTemplate.queryForObject(CHECK_FRIENDSHIP_QUERY, Integer.class, params);
    } //Проверяем дружат ли пользователи

    public void addFriend(Object... params) {
        insert(ADD_FRIEND_QUERY, params);
    } //Добавляем пользователя в друзьям

    public void removeFriend(Object... params) {
        jdbcTemplate.update(REMOVE_FRIEND_QUERY, params);
    } //Удаляем пользователя из друзей

    public Collection<Long> getAllUserFriendsId(Long userId) {
        return jdbcTemplate.queryForList(GET_USER_FRIENDS_ID_QUERY, Long.class, userId);
    } //Получает id всех друзей пользователя

    public Map<Long, Set<Long>> getUserFriends() {
        String sql = "SELECT user_id, friend_id FROM user_friends";

        List<UserFriend> userFriends = jdbcTemplate.query(sql, (rs, rowNum) -> {
            long userId = rs.getLong("user_id");
            long friendId = rs.getLong("friend_id");
            return new UserFriend(userId, friendId);
        });

        Map<Long, Set<Long>> userFriendsMap = new HashMap<>();

        for (UserFriend userFriend : userFriends) {
            userFriendsMap
                    .computeIfAbsent(userFriend.getUserId(), k -> new HashSet<>())
                    .add(userFriend.getFriendId());
        }

        return userFriendsMap;
    } //Получаем id друзей пользователей с привязкой к id пользователя.

    private static class UserFriend {
        private final long userId;
        private final long friendId;

        public UserFriend(long userId, long friendId) {
            this.userId = userId;
            this.friendId = friendId;
        }

        public long getUserId() {
            return userId;
        }

        public long getFriendId() {
            return friendId;
        }
    }

}