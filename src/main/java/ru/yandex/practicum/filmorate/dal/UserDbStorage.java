package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

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

    private static String convertCollectionToIdString(Collection<Long> ids) {
        // Собираем все идентификаторы в строку
        StringBuilder idStringBuilder = new StringBuilder();

        for (Long id : ids) {
            if (!idStringBuilder.isEmpty()) {
                idStringBuilder.append(", ");
            }
            idStringBuilder.append(id);
        }

        return idStringBuilder.toString();
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


        final String FIND_LAST_ID_QUERY = "SELECT MAX(id) FROM users";
        Optional<Long> lastId = getLastId(FIND_LAST_ID_QUERY);


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

            // Если пользователь еще не добавлен, создаем новый объект
            if (!userMap.containsKey(userId)) {
                User user = new User();
                user.setId(userId);
                user.setEmail(rs.getString("email"));
                user.setLogin(rs.getString("login"));
                user.setName(rs.getString("name"));
                user.setBirthday(rs.getDate("birthday").toLocalDate());
                user.setFriends(new HashSet<>()); // Инициализируем пустой набор друзей
                userMap.put(userId, user);
            }

            // Добавляем друга, если он есть
            Long friendId = rs.getLong("friend_id");
            if (!rs.wasNull()) {
                userMap.get(userId).getFriends().add(friendId);
            }
        });

        return new ArrayList<>(userMap.values());
    } // Возвращаем список пользователей

    public List<User> findUsersByIds(Set<Long> userIds) {

        System.out.println("Общие id пользователей " + userIds);
        if (userIds.isEmpty()) {
            return new ArrayList<>(); // Если нет идентификаторов, возвращаем пустой список
        }

        String sql = "SELECT u.id, u.email, u.login, u.name, u.birthday, uf.friend_id " +
                "FROM users u " +
                "LEFT JOIN user_friends uf ON u.id = uf.user_id " +
                "WHERE u.id IN (" + userIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + ")";

        // Используем Map для хранения пользователей, чтобы избежать дубликатов
        Map<Long, User> userMap = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            Long userId = rs.getLong("id");

            // Если пользователь еще не добавлен, создаем новый объект
            if (!userMap.containsKey(userId)) {
                User user = new User();
                user.setId(userId);
                user.setEmail(rs.getString("email"));
                user.setLogin(rs.getString("login"));
                user.setName(rs.getString("name"));
                user.setBirthday(rs.getDate("birthday").toLocalDate());
                user.setFriends(new HashSet<>()); // Инициализируем пустой набор друзей
                userMap.put(userId, user);
            }

            // Добавляем друга, если он есть
            Long friendId = rs.getLong("friend_id");
            if (!rs.wasNull()) {
                userMap.get(userId).getFriends().add(friendId);
            }
        });

        System.out.println("Общие пользователи " + userMap.values());

        return new ArrayList<>(userMap.values());
    }

    public void updateUser(User user) {
        jdbcTemplate.update(UPDATE_QUERY, user.getEmail(), user.getLogin(), user.getName(), user.getBirthday()
                , user.getId());
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

    public Collection<User> getAllUserFriends(Long userId) {

        //Получаем id всех друзей пользователя
        Collection<Long> allUserFriendsId = userFriendDbStorage.getAllUserFriendsId(userId);

        //Получаем всех друзей пользователя
        String usersId = convertCollectionToIdString(allUserFriendsId);

        String FIND_BY_MANY_ID_QUERY = "SELECT * FROM users WHERE id IN (" + usersId + ")";
        List<User> users = findAll(FIND_BY_MANY_ID_QUERY);

        //Получаем таблицу друзей каждого юзера
        Map<Long, Set<Long>> userFriends = userFriendDbStorage.getUserFriends();

        //Добавляем id друзей в объекты user.
        for (User user : users) {
            Collection<Long> friendsId = userFriends.get(user.getId());

            Set<Long> friendsIdSet;
            if (friendsId != null) {
                friendsIdSet = new HashSet<>(friendsId);
            } else {
                friendsIdSet = new HashSet<>();
            }

            user.setFriends(friendsIdSet);
        }

        return users;
    }


}

