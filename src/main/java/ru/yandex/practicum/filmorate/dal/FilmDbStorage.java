package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


@Slf4j
@Repository


public class FilmDbStorage extends BaseDbStorage<Film> {
    private static final int MAX_MPA = 5;
    private static final String INSERT_QUERY = "INSERT INTO films (name, description, duration, release_date) " +
            "VALUES (?, ?, ?, ?)";

    private static final String FIND_BY_ID_QUERY = """
            SELECT f.id, f.name, f.description, f.duration, f.release_date
            FROM films f
            WHERE f.id = ?""";

    private static final String FIND_ALL_QUERY = """
            SELECT f.id, f.name, f.description, f.duration, f.release_date, fl.user_id, 
                   r.id AS rating_id, r.name AS rating_name 
            FROM films f
            LEFT JOIN film_likes fl ON f.id = fl.film_id
            LEFT JOIN film_rating fr ON f.id = fr.film_id
            LEFT JOIN rating r ON fr.rating_id = r.id
            """;

    private static final String UPDATE_QUERY = "UPDATE films SET name = ?, description = ?, duration = ?, release_date = ? " +
            "WHERE id = ?";
    ;

    private static final String GET_ALL_RATINGS_QUERY = "SELECT id, name FROM rating ORDER BY id";
    private static final String GET_RATING_BY_ID_QUERY = "SELECT id, name FROM rating WHERE id = ?";
    private static final String GET_ALL_GENRES_QUERY = "SELECT id, name FROM genres ORDER BY id";
    private static final String GET_GENRE_BY_ID_QUERY = "SELECT id, name FROM genres WHERE id = ?";
    private static final String SELECT_GENRES_QUERY = "SELECT film_id, genre_id FROM film_genres";


    public FilmDbStorage(JdbcTemplate jdbcTemplate, RowMapper<Film> mapper) {
        super(jdbcTemplate, mapper, Film.class);
    }

    public void createFilm(Film film) {

        insert(
                INSERT_QUERY,
                film.getName(),
                film.getDescription(),
                film.getDuration(),
                film.getReleaseDate()
        );

        final String FIND_LAST_ID_QUERY = "SELECT MAX(id) FROM films";
        Optional<Long> lastId = getLastId(FIND_LAST_ID_QUERY);

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

        Collection<Genre> genres = getAllGenres();
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
    } //Добавляем новый фильм в БД.

    public void updateFilm(Film film) {
        jdbcTemplate.update(UPDATE_QUERY, film.getName(), film.getDescription(), film.getDuration(), film.getReleaseDate()
                , film.getId());

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
        // Тянем фильм по id.
        Optional<Film> filmOptional = find(FIND_BY_ID_QUERY, filmId);

        if (filmOptional.isEmpty()) {
            log.error("Фильм с ID {} не найден", filmId);
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }

        Film film = filmOptional.get();

        // Получаем рейтинг по id фильма.
        Mpa mpa = findMpaByFilmId(filmId);
        film.setMpa(mpa);

        // Получаем жанры по id фильма.
        Collection<Genre> genres = findGenresByFilmId(filmId);
        film.setGenres(genres);

        return film;
    }

    public List<Film> findAll() {
        // Используем Map для хранения фильмов, чтобы избежать дубликатов
        Map<Long, Film> filmMap = new HashMap<>();

        jdbcTemplate.query(FIND_ALL_QUERY, rs -> {
            Long filmId = rs.getLong("id");

            // Если фильм еще не добавлен, создаем новый объект
            Film film = filmMap.get(filmId);
            if (film == null) {
                film = new Film();
                film.setId(filmId);
                film.setName(rs.getString("name"));
                film.setDescription(rs.getString("description"));
                film.setDuration(rs.getInt("duration"));
                film.setReleaseDate(rs.getDate("release_date").toLocalDate());
                film.setLikes(new HashSet<>()); // Инициализируем пустой набор лайков
                filmMap.put(filmId, film);
            }

            // Добавляем пользователя, если он есть
            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                film.getLikes().add(userId);
            }

            // Добавляем рейтинг
            Mpa mpa = new Mpa();
            mpa.setId(rs.getInt("rating_id"));

            // Здесь мы извлекаем name для рейтинга
            String mpaName = rs.getString("rating_name"); // Изменение здесь
            mpa.setName(mpaName); // Устанавливаем name для mpa

            if (!rs.wasNull()) {
                film.setMpa(mpa);
            }
        });

        ArrayList<Film> films = new ArrayList<>(filmMap.values());

        // Добавляем к фильмам их жанры.
        Map<Long, Set<Integer>> allFilmGenres = getAllGenresFilm(); //Все жанры с привязкой к конкретному пользователю
        Collection<Genre> AllGenre = getAllGenres();


        for (Film film : films) {

            if (allFilmGenres.containsKey(film.getId())) {

                Collection<Genre> filmGenres = new HashSet<>();
                Set<Integer> allGenresIdFilm = allFilmGenres.get(film.getId());

                for (Integer genreId : allGenresIdFilm) {
                    Genre genre = new Genre();
                    genre.setId(genreId);

                    for (Genre gd : AllGenre) {
                        Long gdId = (long) gd.getId();

                        if (gdId.equals(film.getId())) {
                            genre.setName(gd.getName());
                        }
                    }
                }

                film.setGenres(filmGenres);
            } else {
                film.setGenres(new HashSet<>());
            }
        }

        return films;
    }

    public Collection<Mpa> getAllRatings() {

        return jdbcTemplate.query(GET_ALL_RATINGS_QUERY, (rs, rowNum) -> {
            int id = rs.getInt("id");
            String name = rs.getString("name");

            Mpa mpa = new Mpa();
            mpa.setId(id);
            mpa.setName(name);

            return mpa;
        });
    } //Возвращает все рейтинги из таблицы с рейтингами.

    public Mpa getRating(int ratingID) {
        if (ratingID > MAX_MPA) {
            throw new NotFoundException("Нет рейтинга с таким id");
        }

        return jdbcTemplate.queryForObject(GET_RATING_BY_ID_QUERY, new Object[]{ratingID}, (rs, rowNum) -> {
            int id = rs.getInt("id");
            String name = rs.getString("name");

            Mpa mpa = new Mpa();
            mpa.setId(id);
            mpa.setName(name);

            return mpa;
        });
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
}
