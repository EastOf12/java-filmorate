package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.SQLException;
import java.util.*;

@Slf4j
@Repository
public class UserDbStorage extends BaseDbStorage<User> {
    private static final String FIND_BY_ID_QUERY = "SELECT u.id, u.email, u.login, u.name, u.birthday\n" +
            "FROM users u   \n" +
            "WHERE u.id = ?";
    private static final String INSERT_QUERY = "INSERT INTO users (name, email, login, birthday) " +
            "VALUES (?, ?, ?, ?)";
    private static final String FIND_ALL_QUERY = """
            SELECT u.id, u.email, u.login, u.name, u.birthday, uf.friend_id
            FROM users u
            LEFT JOIN user_friends uf ON u.id = uf.user_id
            """;
    private static final String UPDATE_QUERY = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? " +
            "WHERE id = ?";


    private final UserFriendDbStorage userFriendDbStorage;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbc, UserRowMapper mapper, UserFriendDbStorage userFriendDbStorage) {
        super(jdbc, mapper, User.class);
        this.userFriendDbStorage = userFriendDbStorage;
    }

    //Создает нового пользователя
    public void createUser(User user) {

        insert(
                INSERT_QUERY,
                user.getName(),
                user.getEmail(),
                user.getLogin(),
                user.getBirthday()
        );


        Optional<Long> lastId = getLastId();


        if (lastId.isPresent()) {
            user.setId(lastId.get()); //Устанавливаем id пользователя, которое получили от БД
        } else {
            throw new NotFoundException("Не найден ни один пользователь в БД");
        }


        log.info("Пользователь {} сохранен в базе данных", user.getId());
    }

    public List<User> findAll() {
        // Используем Map для хранения пользователей, чтобы избежать дубликатов
        Map<Long, User> userMap = new HashMap<>();

        jdbcTemplate.query(FIND_ALL_QUERY, rs -> {
            Long userId = rs.getLong("id");

            // Создаем или получаем существующего пользователя
            User user = userMap.computeIfAbsent(userId, key -> {
                try {
                    return new UserRowMapper().mapRow(rs, 0);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            // Добавляем друга, если он есть
            Long friendId = rs.getLong("friend_id");
            if (!rs.wasNull()) {
                assert user != null;
                user.getFriends().add(friendId);
            }
        });

        return new ArrayList<>(userMap.values());
    }

    public void updateUser(User user) {
        jdbcTemplate.update(UPDATE_QUERY, user.getEmail(), user.getLogin(), user.getName(), user.getBirthday(),
                user.getId());
    }

    public User findById(Long id) {

        //Тянем пользователя по id.
        Optional<User> userOptional = find(FIND_BY_ID_QUERY, id);

        if (userOptional.isEmpty()) {
            log.error("Пользователь с ID {} не найден", id);
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }

        User user = userOptional.get();

        //Получаем id друзей пользователя.
        user.setFriends(new HashSet<>(userFriendDbStorage.getAllUserFriendsId(id)));

        return user;
    }

    public List<User> getAllUserFriends(Long userId) {
        Map<Long, User> userMap = new HashMap<>();

        final String FIND_USER_FRIENDS_QUERY =
                "SELECT u.id, u.email, u.login, u.name, u.birthday, uf.friend_id " +
                        "FROM users u " +
                        "LEFT JOIN user_friends uf ON u.id = uf.user_id " +
                        "WHERE u.id IN (SELECT friend_id FROM user_friends WHERE user_id = ?)";

        jdbcTemplate.query(FIND_USER_FRIENDS_QUERY, rs -> {
            Long userIdFromDb = rs.getLong("id");

            // Создаем или получаем существующего пользователя
            User user = userMap.computeIfAbsent(userIdFromDb, key -> {
                try {
                    return new UserRowMapper().mapRow(rs, 0);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            // Добавляем друга, если он есть
            Long friendId = rs.getLong("friend_id");
            if (!rs.wasNull()) {
                assert user != null;
                user.getFriends().add(friendId);
            }
        }, userId);

        return new ArrayList<>(userMap.values());
    }

    public List<User> getFriendsCommon(Long userId, Long otherUserId) {
        Map<Long, User> userMap = new HashMap<>();

        // Запрос для получения общих друзей
        final String FIND_COMMON_FRIENDS_QUERY =
                "SELECT u.id, u.email, u.login, u.name, u.birthday, uf.friend_id " +
                        "FROM users u " +
                        "LEFT JOIN user_friends uf ON u.id = uf.user_id " +
                        "WHERE u.id IN (SELECT friend_id FROM user_friends WHERE user_id = ?) AND u.id IN (SELECT friend_id FROM user_friends WHERE user_id = ?)";


        jdbcTemplate.query(FIND_COMMON_FRIENDS_QUERY, rs -> {
            Long userIdFromDb = rs.getLong("id");

            User user = userMap.computeIfAbsent(userIdFromDb, key -> {
                try {
                    User mappedUser = new UserRowMapper().mapRow(rs, 0);
                    assert mappedUser != null;
                    mappedUser.setFriends(new HashSet<>());
                    return mappedUser;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            Long friendId = rs.getLong("friend_id");
            if (!rs.wasNull()) {
                user.getFriends().add(friendId);
            }
        }, otherUserId, userId);

        return new ArrayList<>(userMap.values());
    }

    public Optional<Long> getLastId(Object... params) {
        final String FIND_LAST_ID_QUERY = "SELECT MAX(id) FROM users";
        return Optional.ofNullable(jdbcTemplate.queryForObject(FIND_LAST_ID_QUERY, Long.class, params));
    } //Получаем последний id в таблице
}

