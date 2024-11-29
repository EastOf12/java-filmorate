package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.SQLException;
import java.time.LocalDate;
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
    public User createUser(User user) {
        log.trace("Получен запрос на добавление нового пользователя");

        //Проходим валидацию полей.
        passValidationCreate(user);
        log.debug("Валидация пройдена.");

        Long lastId = insertGetId(
                INSERT_QUERY,
                user.getName(),
                user.getEmail(),
                user.getLogin(),
                user.getBirthday()
        );

        user.setId(lastId); //Устанавливаем id пользователя, которое получили от БД

        log.info("Пользователь {} сохранен в базе данных", user.getId());
        return user;
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

        log.info("Предали информацию по все доступным пользователям.");
        return new ArrayList<>(userMap.values());
    }

    public User updateUser(User updateUser) {
        log.trace("Получен запрос на обновление пользователя");

        //Проверяем корректность переданного ID
        if (updateUser.getId() == null) {
            log.warn("Валидация не пройдена. Id должен быть указан");
            throw new ValidationException("Id должен быть указан");
        }

        User user = findById(updateUser.getId());

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
        jdbcTemplate.update(UPDATE_QUERY, user.getEmail(), user.getLogin(), user.getName(), user.getBirthday(),
                user.getId());

        log.info("Обновлен пользователь {}", updateUser.getId());
        return user;
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

    public void addFriend(Long userId, Long friendId) {

        //Проверяем, что такие пользователи существуют в бд и указаны корректно
        if (userId.equals(friendId)) {
            log.warn("Пользователь с id {} хочет добавить в друзья сам себя", userId);
            throw new NotFoundException("Нельзя добавить в друзья самого себя");
        }

        User user = findById(userId);
        User otherUser = findById(friendId);


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

    public void removeFriend(Long userId, Long friendId) {

        //Проверяем, что такие пользователи существуют и являются друзьями
        User user = findById(userId);
        User otherUser = findById(friendId);

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

    private boolean checkFriendship(Long userId, Long friendId) {
        return userFriendDbStorage.checkFriendship(userId, friendId) == 1;
    } //Проверяем, являются ли пользователи друзьями.
}

