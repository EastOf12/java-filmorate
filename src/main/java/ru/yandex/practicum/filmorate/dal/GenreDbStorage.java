package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;

@Slf4j
@Repository
public class GenreDbStorage extends BaseDbStorage<Film> {
    private static final String GET_ALL_GENRES_QUERY = "SELECT id, name FROM genres ORDER BY id";
    private static final String GET_GENRE_BY_ID_QUERY = "SELECT id, name FROM genres WHERE id = ?";

    public GenreDbStorage(JdbcTemplate jdbcTemplate, FilmRowMapper mapper) {
        super(jdbcTemplate, mapper, Film.class);
    }

    public Collection<Genre> getAllGenres() {
        return jdbcTemplate.query(GET_ALL_GENRES_QUERY, (rs, rowNum) -> {
            int id = rs.getInt("id");
            String name = rs.getString("name");

            Genre genre = new Genre();
            genre.setId(id);
            genre.setName(name);

            return genre;
        });

    } //Возвращает все жанры

    public Genre getGenre(int genreID) {
        return jdbcTemplate.queryForObject(GET_GENRE_BY_ID_QUERY, new Object[]{genreID}, (rs, rowNum) -> {
            int id = rs.getInt("id");
            String name = rs.getString("name");

            Genre genre = new Genre();
            genre.setId(id);
            genre.setName(name);

            return genre;
        });
    }
}

