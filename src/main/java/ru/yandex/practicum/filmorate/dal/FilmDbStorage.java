package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.InMemoryGenreStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
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
             SELECT f.id, f.name, f.description, f.duration, f.release_date, fl.user_id
             , r.id AS rating_id, r.name AS rating_name\s
             FROM films f
             LEFT JOIN film_likes fl ON f.id = fl.film_id
             LEFT JOIN film_rating fr ON f.id = fr.film_id
             LEFT JOIN rating r ON fr.rating_id = r.id
            \s""";
    private static final String FIND_ALL_QUERY_POPULAR = """
             SELECT f.id, f.name, f.description, f.duration, f.release_date,
                    COUNT(fl.user_id) AS like_count,
                    r.id AS rating_id, r.name AS rating_name
             FROM films f
             LEFT JOIN film_likes fl ON f.id = fl.film_id
             LEFT JOIN film_rating fr ON f.id = fr.film_id
             LEFT JOIN rating r ON fr.rating_id = r.id
             GROUP BY f.id, f.name, f.description, f.duration, f.release_date, r.id, r.name
             ORDER BY like_count DESC
            """;
    private static final String UPDATE_QUERY = "UPDATE films SET name = ?, description = ?, duration = ?, release_date = ? " +
            "WHERE id = ?";
    private static final String SELECT_GENRES_QUERY = "SELECT film_id, genre_id FROM film_genres";
    private final InMemoryGenreStorage inMemoryGenreStorage;


    public FilmDbStorage(JdbcTemplate jdbcTemplate, FilmRowMapper mapper, InMemoryGenreStorage inMemoryGenreStorage) {
        super(jdbcTemplate, mapper, Film.class);
        this.inMemoryGenreStorage = inMemoryGenreStorage;
    }

    public Film createFilm(Film film) {

        insert(
                INSERT_QUERY,
                film.getName(),
                film.getDescription(),
                film.getDuration(),
                film.getReleaseDate()
        );


        Optional<Long> lastId = getLastId();

        if (lastId.isPresent()) {
            film.setId(lastId.get()); //Устанавливаем id фильма, которое получили от БД
        } else {
            throw new NotFoundException("Не найден ни один фильм в БД");
        }

        //Добавляем рейтинг к фильму
        addRating(film.getId(), film.getMpa().getId());

        //Добавляем жанры к фильму
        Set<Integer> allGenresId = new HashSet<>();
        for (Genre genre : film.getGenres()) {
            allGenresId.add(genre.getId());
        }
        addGenres(film.getId(), allGenresId);

        Collection<Genre> genres = inMemoryGenreStorage.getAllGenres();
        List<Genre> filmGenres = new ArrayList<>();

        for (Genre genre : genres) {
            for (Integer genreId : allGenresId) {
                if (Objects.equals(genreId, genre.getId())) {
                    filmGenres.add(genre);
                }
            }
        }

        filmGenres.sort((g1, g2) -> Integer.compare(g1.getId(), g2.getId()));
        film.setGenres(filmGenres);

        log.info("Фильм {} сохранен в базе данных", film.getId());

        return film;
    } //Добавляем новый фильм в БД.

    public void updateFilm(Film film) {
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

    public List<Film> findAllPopular() {
        List<Film> films = new ArrayList<>();

        jdbcTemplate.query(FIND_ALL_QUERY_POPULAR, rs -> {
            Film film = new FilmRowMapper().mapRow(rs, 0);

            // Получаем количество лайков
            int likeCount = rs.getInt("like_count");
            Objects.requireNonNull(film).setLikes(new HashSet<>(likeCount)); // Устанавливаем размер коллекции лайков

            // Обработка рейтинга
            if (rs.getInt("rating_id") != 0) {
                Mpa mpa = new Mpa();
                mpa.setId(rs.getInt("rating_id"));
                mpa.setName(rs.getString("rating_name")); // Устанавливаем имя для mpa
                film.setMpa(mpa);
            }

            films.add(film);
        });

        return films; // Возвращаем список фильмов
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

        return new ArrayList<>(filmMap.values()); // Возвращаем список фильмов
    }

    private void addRating(Long filmID, int ratingID) {
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


    private void addGenres(Long filmId, Set<Integer> genreIds) {

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

    private HashMap<Long, Set<Integer>> getAllGenresFilm() {

        HashMap<Long, Set<Integer>> filmGenresMap = new HashMap<>();

        // Выполняем запрос и обрабатываем результат
        jdbcTemplate.query(SELECT_GENRES_QUERY, new RowMapper<Void>() {
            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                Long filmId = rs.getLong("film_id");
                Integer genreId = rs.getInt("genre_id");

                filmGenresMap.computeIfAbsent(filmId, id -> new HashSet<>()).add(genreId);

                return null;
            }
        });

        return filmGenresMap;
    }

    // Метод для получения MPA (рейтинг) фильма
    private Mpa findMpaByFilmId(Long filmId) {
        String query = "SELECT r.id, r.name FROM film_rating fr " +
                "JOIN rating r ON fr.rating_id = r.id " +
                "WHERE fr.film_id = ?";
        return jdbcTemplate.queryForObject(query, (rs, rowNum) ->
                new Mpa(rs.getInt("id"), rs.getString("name")), filmId);
    }

    // Метод для получения жанров фильма
    private Collection<Genre> findGenresByFilmId(Long filmId) {
        String query = "SELECT g.id, g.name FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id = ?";
        return jdbcTemplate.query(query, (rs, rowNum) ->
                new Genre(rs.getInt("id"), rs.getString("name")), filmId);
    }

    public Optional<Long> getLastId(Object... params) {
        final String FIND_LAST_ID_QUERY = "SELECT MAX(id) FROM films";
        return Optional.ofNullable(jdbcTemplate.queryForObject(FIND_LAST_ID_QUERY, Long.class, params));
    } //Получаем последний id в таблице
}
