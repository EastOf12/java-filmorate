package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;


@Slf4j
@Repository
public class FilmDbStorage extends BaseDbStorage<Film> {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final int MAX_GENRE = 6;
    private static final Map<Integer, String> allMpa = Map.of(
            1, "G",
            2, "PG",
            3, "PG-13",
            4, "R",
            5, "NC-17"
    );
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
                   r.id AS rating_id, r.name AS rating_name,
                   g.id AS genre_id, g.name AS genre_name
            FROM films f
            LEFT JOIN film_likes fl ON f.id = fl.film_id
            LEFT JOIN film_rating fr ON f.id = fr.film_id
            LEFT JOIN rating r ON fr.rating_id = r.id
            LEFT JOIN film_genres fg ON f.id = fg.film_id
            LEFT JOIN genres g ON fg.genre_id = g.id
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
    private static final String SELECT_GENRES_QUERY = "SELECT film_id, genre_id FROM film_genres";
    private final GenreDbStorage genreDbStorage;
    private final LikesDbStorage likesDbStorage;
    private final UserDbStorage userDbStorage;


    public FilmDbStorage(JdbcTemplate jdbcTemplate, FilmRowMapper mapper, GenreDbStorage genreDbStorage, LikesDbStorage likesDbStorage, UserDbStorage userDbStorage) {
        super(jdbcTemplate, mapper, Film.class);
        this.genreDbStorage = genreDbStorage;
        this.likesDbStorage = likesDbStorage;
        this.userDbStorage = userDbStorage;
    }

    public Film createFilm(Film film) {
        log.trace("Получен запрос на добавление нового фильма");

        //Проходим валидацию полей.
        passValidationCreate(film);
        log.debug("Валидация пройдена.");

        Long lastId = insertGetId(
                INSERT_QUERY,
                film.getName(),
                film.getDescription(),
                film.getDuration(),
                film.getReleaseDate()
        );

        film.setId(lastId); //Устанавливаем id фильма, которое получили от БД

        //Добавляем рейтинг к фильму
        addRating(film.getId(), film.getMpa().getId());

        //Добавляем жанры к фильму
        Set<Integer> allGenresId = new HashSet<>();
        for (Genre genre : film.getGenres()) {
            allGenresId.add(genre.getId());
        }
        addGenres(film.getId(), allGenresId);

        Collection<Genre> genres = genreDbStorage.getAllGenres();
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

    public Film updateFilm(Film updateFilm) {

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

            //Обработка жанров
            if (rs.getInt("genre_id") != 0) {
                Genre genre = new Genre();
                genre.setId(rs.getInt("genre_id"));
                genre.setName(rs.getString("genre_name"));
                film.getGenres().add(genre);
            }
        });

        log.info("Отправили информацию по все фильмам с фильтрацией по id");
        return new ArrayList<>(filmMap.values()); // Возвращаем список фильмов
    }

    public void addLike(Long filmId, Long userId) {

        //Проверяем, что пользователь и фильм существуют.
        userDbStorage.findById(userId);
        findById(filmId);

        //Добавляем лайк в БД.
        likesDbStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {

        //Проверяем, что фильм и пользователь существуют
        User user = userDbStorage.findById(userId);
        Film film = findById(filmId);

        if (user == null) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        if (film == null) {
            log.warn("Нет фильма с id {}", filmId);
            throw new NotFoundException("Нет фильма с id " + filmId);
        }

        if (checkLike(filmId, userId)) {
            likesDbStorage.removeLike(userId, filmId);
            log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
        } else {
            log.warn("Пользователь {} не ставил лайк фильму {}", userId, filmId);
            throw new NotFoundException("Пользователь " + userId + " не ставил лайк фильму " + filmId);
        }
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

    private void passValidationCreate(Film film) {
        //Проверяем корректность заполнения полей.
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Валидация не пройдена. Не было передано название фильма.");
            throw new ValidationException("Название не может быть пустым.");
        } else if (film.getDescription() == null || film.getDescription().isBlank()) {
            log.warn("Валидация не пройдена. Описание запроса не может быть пустым");
            throw new ValidationException("Описание запроса не может быть пустым");
        } else if (film.getDescription().length() > 200) {
            log.warn("Валидация не пройдена. Количество символов в описании запроса {}",
                    film.getDescription().length());
            throw new ValidationException("Максимальная длина описания — 200 символов.");
        } else if (film.getReleaseDate() == null) {
            log.warn("Валидация не пройдена. Дата в запросе = null");
            throw new ValidationException("Дата в запросе = null");
        } else if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Валидация не пройдена. Дата в запросе {}",
                    film.getReleaseDate());
            throw new ValidationException("Дата релиза — не раньше " + MIN_RELEASE_DATE);
        } else if (film.getDuration() < 1) {
            log.warn("Валидация не пройдена. Продолжительность фильма в запросе {}",
                    film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        } else if (areGenreMoreRange(film.getGenres())) {
            log.warn("Валидация не пройдена. Передан несуществующий жанр");
            throw new ValidationException("Передан несуществующий жанр");
        } else if (isMpaValidationCorrect(film.getMpa())) {
            film.getMpa().setName(allMpa.get(film.getMpa().getId()));
        }
    }

    private boolean checkLike(Long filmId, Long userId) {
        return likesDbStorage.checkLike(userId, filmId) == 1;
    } //Проверяем, ставил ли пользователь лайк.

    private boolean areGenreMoreRange(Collection<Genre> genres) {

        for (Genre genre : genres) {
            if (genre.getId() <= 0 || genre.getId() > MAX_GENRE) {
                return true;
            }
        }

        return false;
    } //Проверяем, что переданные id жанра существуют.

    private boolean isMpaValidationCorrect(Mpa mpa) {
        final int MAX_MPA_ID = 5;

        if (mpa.getId() > MAX_MPA_ID || mpa.getId() < 1) {
            log.error("Такой рейтинг не существует");
            throw new ValidationException("Передан несуществующий mpa.id");
        }

        return true;
    }
}
