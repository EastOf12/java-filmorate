package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Repository
public class FilmDbStorage extends BaseDbStorage<Film> {
    private static final String INSERT_QUERY = "INSERT INTO films (name, description, duration, release_date) " +
            "VALUES (?, ?, ?, ?)";
    private static final String FIND_BY_ID_QUERY = """
            SELECT f.id, f.name, f.description, f.duration, f.release_date,
                   r.id AS rating_id, r.name AS rating_name
            FROM films f
            LEFT JOIN film_rating fr ON f.id = fr.film_id
            LEFT JOIN rating r ON fr.rating_id = r.id
            WHERE f.id = ?""";
    private static final String FIND_ALL_QUERY = """
            SELECT f.id, f.name, f.description, f.duration, f.release_date, fl.user_id,
                   r.id AS rating_id, r.name AS rating_name
            FROM films f
            LEFT JOIN film_likes fl ON f.id = fl.film_id
            LEFT JOIN film_rating fr ON f.id = fr.film_id
            LEFT JOIN rating r ON fr.rating_id = r.id
            """;
    private static final String FIND_ALL_QUERY_POPULAR = """
            SELECT f.id, f.name, f.description, f.duration, f.release_date, fl.user_id,
                      r.id AS rating_id, r.name AS rating_name,
                      g.id AS genre_id, g.name AS genre_name
               FROM films f
               LEFT JOIN film_likes fl ON f.id = fl.film_id
               LEFT JOIN film_rating fr ON f.id = fr.film_id
               LEFT JOIN rating r ON fr.rating_id = r.id
               LEFT JOIN film_genres fg ON f.id = fg.film_id
               LEFT JOIN genres g ON fg.genre_id = g.id
               GROUP BY f.id, f.name, f.description, f.duration, f.release_date, fl.user_id,\s
                        r.id, r.name, g.id
               ORDER BY COUNT(fl.user_id) DESC
               LIMIT ?
            """;
    private static final String UPDATE_QUERY = "UPDATE films SET name = ?, description = ?, duration = ?, release_date = ? " +
            "WHERE id = ?";

    public FilmDbStorage(JdbcTemplate jdbcTemplate, FilmRowMapper mapper, GenreDbStorage genreDbStorage, LikesDbStorage likesDbStorage, UserDbStorage userDbStorage) {
        super(jdbcTemplate, mapper, Film.class);
    }

    public Film createFilm(Film film) {
        log.trace("Получен запрос на добавление нового фильма");

        //Проходим валидацию полей.
        log.debug("Валидация пройдена.");

        Long lastId = insertGetId(
                INSERT_QUERY,
                film.getName(),
                film.getDescription(),
                film.getDuration(),
                film.getReleaseDate()
        );

        film.setId(lastId); //Устанавливаем id фильма, которое получили от БД
        log.info("Фильм {} сохранен в базе данных", film.getId());
        return film;
    } //Добавляем новый фильм в БД.

    public Film updateFilm(Film updateFilm) {
        final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

        log.trace("Получен запрос на обновление информации по фильму");

        //Проверяем корректность переданного ID
        if (updateFilm.getId() == null) {
            log.warn("Валидация не пройдена. Id должен быть указан");
            throw new ValidationException("Id должен быть указан");
        }

        //Достаем фильм по его id
        Film film = findById(updateFilm.getId());

        //Валидируем поля
        if (updateFilm.getName() != null && !updateFilm.getName().isBlank()) {
            film.setName(updateFilm.getName());
        }

        if (updateFilm.getDescription() != null && !updateFilm.getDescription().isBlank() && updateFilm
                .getDescription().length() <= 200) {
            film.setDescription(updateFilm.getDescription());
        }

        if (updateFilm.getReleaseDate() != null && !updateFilm.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            film.setReleaseDate(updateFilm.getReleaseDate());
        }

        if (updateFilm.getDuration() > 0) {
            film.setDuration(updateFilm.getDuration());
        }


        if (Objects.nonNull(updateFilm.getGenres())) {
            film.setGenres(updateFilm.getGenres());
        }

        jdbcTemplate.update(UPDATE_QUERY, film.getName(), film.getDescription(), film.getDuration(), film.getReleaseDate(),
                film.getId());

        if (film.getMpa() != null) {
            updateRating(film.getId(), film.getMpa().getId());
        }

        //Обновляем id жанров
        if (film.getGenres() != null) {
            Set<Integer> allGenresId = new HashSet<>();
            for (Genre genre : film.getGenres()) {
                allGenresId.add(genre.getId());
            }

            updateGenres(film.getId(), allGenresId);
        }

        return film;
    } //Обновляем фильм в БД.

    public Film findById(Long filmId) {
        // Запрос для получения фильма с MPA
        Optional<Film> filmOptional = jdbcTemplate.query(FIND_BY_ID_QUERY, rs -> {
            Film film = null;

            while (rs.next()) {
                if (film == null) {
                    film = new FilmRowMapper().mapRow(rs, 0);
                }

                // Извлекаем MPA, если рейтинг существует
                if (rs.getInt("rating_id") != 0) {
                    Mpa mpa = new Mpa();
                    mpa.setId(rs.getInt("rating_id"));
                    mpa.setName(rs.getString("rating_name")); // Извлекаем имя рейтинга
                    Objects.requireNonNull(film).setMpa(mpa); // Устанавливаем MPA для фильма
                }
            }

            return Optional.ofNullable(film);
        }, filmId);

        // Проверяем, найден ли фильм, и выбрасываем исключение, если его нет
        if (Objects.requireNonNull(filmOptional).isEmpty()) {
            log.error("Фильм с ID {} не найден", filmId);
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }

        Film film = filmOptional.get();

        // Получаем жанры по id фильма.
        Collection<Genre> genres = findGenresByFilmId(filmId);
        film.setGenres(genres);

        return film;
    }

    public List<Film> findAllPopular(int limit) {
        LinkedHashMap<Long, Film> filmMap = new LinkedHashMap<>();

        jdbcTemplate.query(FIND_ALL_QUERY_POPULAR, rs -> {
            Long filmId = rs.getLong("id");

            Film film = filmMap.computeIfAbsent(filmId, key -> {
                Film mappedFilm;
                try {
                    mappedFilm = new FilmRowMapper().mapRow(rs, 0);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                assert mappedFilm != null;
                mappedFilm.setLikes(new HashSet<>());
                return mappedFilm;
            });

            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                film.getLikes().add(userId);
            }

            if (rs.getInt("rating_id") != 0) {
                Mpa mpa = new Mpa();
                mpa.setId(rs.getInt("rating_id"));
                mpa.setName(rs.getString("rating_name"));
                film.setMpa(mpa);
            }

            if (rs.getInt("genre_id") != 0) {
                Genre genre = new Genre();
                genre.setId(rs.getInt("genre_id"));
                genre.setName(rs.getString("genre_name"));
                film.getGenres().add(genre);
            }
        }, limit);

        List<Film> films = new ArrayList<>(filmMap.values());
        films.sort((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()));

        log.info("Отправили информацию по всем фильмам, отсортированным по количеству лайков");
        return films;
    }

    public List<Film> findAll() {
        // Используем Map для хранения фильмов, чтобы избежать дубликатов
        Map<Long, Film> filmMap = new HashMap<>();

        jdbcTemplate.query(FIND_ALL_QUERY, rs -> {
            Long filmId = rs.getLong("id");

            // Сначала выполним отображение через FilmRowMapper
            Film film = filmMap.computeIfAbsent(filmId, key -> {
                Film mappedFilm;
                try {
                    mappedFilm = new FilmRowMapper().mapRow(rs, 0);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                assert mappedFilm != null;
                mappedFilm.setLikes(new HashSet<>());
                return mappedFilm;
            });

            // Добавляем пользователя, если он есть
            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                film.getLikes().add(userId);
            }

            // Обработка рейтинга
            if (rs.getInt("rating_id") != 0) {
                Mpa mpa = new Mpa();
                mpa.setId(rs.getInt("rating_id"));
                mpa.setName(rs.getString("rating_name")); // Устанавливаем имя для mpa
                film.setMpa(mpa);
            }
        });

        log.info("Отправили информацию по все фильмам с фильтрацией по id");
        return new ArrayList<>(filmMap.values()); // Возвращаем список фильмов
    }

    public void addRating(Long filmID, int ratingID) {
        final String ADD_RATING_QUERY = "INSERT INTO film_rating (film_id, rating_id) VALUES (?, ?)";
        jdbcTemplate.update(ADD_RATING_QUERY, filmID, ratingID);
    } //Добавляем рейтинг на фильм

    private void updateRating(Long filmID, int ratingID) {
        final String UPDATE_RATING_QUERY = "UPDATE film_rating SET rating_id = ? WHERE film_id = ?";

        int rowsAffected = jdbcTemplate.update(UPDATE_RATING_QUERY, ratingID, filmID);

        if (rowsAffected == 0) {
            log.warn("Не удалось обновить рейтинг для фильма с ID {}. Фильм не найден.", filmID);
            throw new NoSuchElementException("Фильм с ID " + filmID + " не найден для обновления рейтинга.");
        }
    }

    public void addGenres(Long filmId, Set<Integer> genreIds) {

        final String INSERT_GENRE_QUERY = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        Integer[] genreIdArray = genreIds.toArray(new Integer[0]);

        jdbcTemplate.batchUpdate(INSERT_GENRE_QUERY, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, filmId);
                ps.setInt(2, genreIdArray[i]);
            }

            @Override
            public int getBatchSize() {
                return genreIdArray.length;
            }
        });
    } //Добавляет жанры на фильм

    private void updateGenres(Long filmId, Set<Integer> genreIds) {

        //Удаляем уже установленные жанры для фильма
        final String DELETE_GENRES_QUERY = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(DELETE_GENRES_QUERY, filmId);

        //Устанавливаем новые жанры
        addGenres(filmId, genreIds);
    }

    // Метод для получения жанров фильма
    private Collection<Genre> findGenresByFilmId(Long filmId) {
        String query = "SELECT g.id, g.name FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id = ?";
        return jdbcTemplate.query(query, (rs, rowNum) ->
                new Genre(rs.getInt("id"), rs.getString("name")), filmId);
    }
}
