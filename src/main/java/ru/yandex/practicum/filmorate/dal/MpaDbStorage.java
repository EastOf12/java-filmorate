package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.Collection;


@Slf4j
@Repository
public class MpaDbStorage extends BaseDbStorage<Film> {
    private static final String GET_ALL_MPA_QUERY = "SELECT id, name FROM rating ORDER BY id";
    private static final String GET_MPA_BY_ID_QUERY = "SELECT id, name FROM rating WHERE id = ?";
    private static final int MAX_MPA = 5;

    public MpaDbStorage(JdbcTemplate jdbcTemplate, FilmRowMapper mapper) {
        super(jdbcTemplate, mapper, Film.class);
    }

    public Collection<Mpa> getAllMpa() {

        return jdbcTemplate.query(GET_ALL_MPA_QUERY, (rs, rowNum) -> {
            int id = rs.getInt("id");
            String name = rs.getString("name");

            Mpa mpa = new Mpa();
            mpa.setId(id);
            mpa.setName(name);

            return mpa;
        });
    } //Возвращает все рейтинги из таблицы с рейтингами.

    public Mpa getMpa(int ratingID) {
        if (ratingID > MAX_MPA) {
            throw new NotFoundException("Нет рейтинга с таким id");
        }

        return jdbcTemplate.queryForObject(GET_MPA_BY_ID_QUERY, new Object[]{ratingID}, (rs, rowNum) -> {
            int id = rs.getInt("id");
            String name = rs.getString("name");

            Mpa mpa = new Mpa();
            mpa.setId(id);
            mpa.setName(name);

            return mpa;
        });
    }
}
