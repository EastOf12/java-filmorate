package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Film;

@Slf4j
@Repository
public class LikesDbStorage extends BaseDbStorage<Film> {
    private static final String ADD_FRIEND_QUERY = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
    private static final String CHECK_LIKE_QUERY = """
            SELECT COUNT(*) AS likes
            FROM film_likes
            WHERE user_id = ? AND film_id = ?;""";
    private static final String REMOVE_LIKE_QUERY = """
            DELETE FROM film_likes
            WHERE user_id = ? AND film_id = ?""";

    public LikesDbStorage(JdbcTemplate jdbcTemplate, FilmRowMapper mapper) {
        super(jdbcTemplate, mapper, Film.class);
    }

    public void addLike(Object... params) {
        insert(ADD_FRIEND_QUERY, params);
    }

    public void removeLike(Object... params) {
        try {
            jdbcTemplate.update(REMOVE_LIKE_QUERY, params);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    } //Удаляем пользователя из друзей

    public Integer checkLike(Object... params) {
        return jdbcTemplate.queryForObject(CHECK_LIKE_QUERY, Integer.class, params);
    } //Проверяем, ставил ли пользователь лайк фильму
}
