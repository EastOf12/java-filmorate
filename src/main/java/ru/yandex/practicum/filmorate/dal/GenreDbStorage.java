package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.*;

@Slf4j
@Repository
public class GenreDbStorage extends BaseDbStorage<Film> {
    private static final String GET_ALL_GENRES_QUERY = "SELECT id, name FROM genres ORDER BY id";
    private static final String GET_GENRE_BY_ID_QUERY = "SELECT id, name FROM genres WHERE id = ?";
    private static final int MAX_GENRE = 6;

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

        if (MAX_GENRE < genreID) {
            log.warn("Валидация не пройдена. Передан несуществующий жанр");
            throw new NotFoundException("Передан несуществующий жанр");
        }

        return jdbcTemplate.queryForObject(GET_GENRE_BY_ID_QUERY, new Object[]{genreID}, (rs, rowNum) -> {
            int id = rs.getInt("id");
            String name = rs.getString("name");

            Genre genre = new Genre();
            genre.setId(id);
            genre.setName(name);

            return genre;
        });
    }

    public List<Film> addGenres(List<Film> films) {
        if (films.isEmpty()) {
            return films;
        }

        String inSql = String.join(",", Collections.nCopies(films.size(), "?"));

        List<Long> filmIds = films.stream().map(Film::getId).toList();

        final String sqlQuery = "SELECT fg.film_id, g.id AS genre_id, g.name AS genre_name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id IN (" + inSql + ")";

        Map<Long, Set<Genre>> filmGenresMap = new HashMap<>();

        jdbcTemplate.query(sqlQuery, (rs) -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = new Genre(rs.getInt("genre_id"), rs.getString("genre_name"));

            filmGenresMap
                    .computeIfAbsent(filmId, k -> new HashSet<>())
                    .add(genre);
        }, filmIds.toArray());

        films.forEach(film -> {
            Set<Genre> genres = filmGenresMap.getOrDefault(film.getId(), new HashSet<>());
            film.setGenres(genres); // Устанавливаем жанры фильму
        });

        return films;
    }

}

