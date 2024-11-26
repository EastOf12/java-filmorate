package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.LikesDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dal.UserFriendDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmRowMapper.class, FilmDbStorage.class, InMemoryFilmStorage.class, UserDbStorage.class,
        UserRowMapper.class, UserFriendDbStorage.class, LikesDbStorage.class, InMemoryUserStorage.class})


public class InMemoryFilmStorageTest {
    private final InMemoryFilmStorage inMemoryFilmStorage;
    private final InMemoryUserStorage inMemoryUserStorage;
    private Film film;

    @BeforeEach
    public void beforeEachFile() throws IOException {

        //Создаем объект фильма с правильными параметрами.
        film = new Film();
        film.setName("Человек паук");
        film.setDescription("Человека укусил паук и тот стал супер героем.");
        film.setReleaseDate(LocalDate.of(2004, 12, 12));
        film.setDuration(1000);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        mpa.setName("G");
        film.setMpa(mpa);

        Collection<Genre> genres = new HashSet<>();
        film.setGenres(genres);

    }


    @Test
    public void shouldReturnPositiveWhenCreateFilmIsCorrect() {
        //Добавляем фильм
        inMemoryFilmStorage.create(film);

        //Проверяем, что фильм с правильными успешно добавлен.
        assertEquals(1, inMemoryFilmStorage.getAll().size(), "Должен быть 1 фильм");
        assertEquals(film, inMemoryFilmStorage.getFilm(film.getId()),
                "Фильмы должны быть равны");

    } //Проверяем корректность добавления фильма с правильными параметрами.

    @Test
    public void shouldReturnPositiveWhenUpdateFilmIsCorrect() {

        //Добавляем фильм
        Film cratedFilm = inMemoryFilmStorage.create(film);
        assertEquals(1, inMemoryFilmStorage.getAll().size());

        //Создаем объект фильма для обновления.
        Film filmNew = new Film();
        filmNew.setName("Новый человек паук");
        filmNew.setDescription("Человека укусил паук и тот стал супер героем.");
        filmNew.setReleaseDate(LocalDate.of(2024, 12, 12));
        filmNew.setDuration(1000);
        filmNew.setId(cratedFilm.getId());
        Mpa mpa = new Mpa();
        mpa.setId(1);
        mpa.setName("G");
        filmNew.setMpa(mpa);

        Collection<Genre> genres = new HashSet<>();
        filmNew.setGenres(genres);

        //Обновляем фильм
        inMemoryFilmStorage.update(filmNew);

        //Проверяем что фильм по прежнему 1.
        assertEquals(1, inMemoryFilmStorage.getAll().size());

        //Проверяем, что фильм обновлен.
        assertNotEquals(filmNew, inMemoryFilmStorage.getFilm(film.getId()),
                "Фильмы не должны быть одинаковыми.");
    } //Проверяем корректность обновления фильма с правильными параметрами.

    @Test
    public void shouldReturnPositiveWhenNameValidationIsCorrect() {
        //Делаем название фильма некорректным и проверяем валидацию.
        film.setName(" ");
        assertThrows(ValidationException.class, () -> inMemoryFilmStorage.create(film),
                "Не выброшено исключение ValidationException");
        film.setName(null);
        assertThrows(ValidationException.class, () -> inMemoryFilmStorage.create(film),
                "Не выброшено исключение ValidationException");

        //Проверяем, что фильм по прежнему не создан
        assertEquals(0, inMemoryFilmStorage.getAll().size(), "Фильм не должен быть создан");
    } //Проверяем корректность работы валидации на название фильма

    @Test
    public void shouldReturnPositiveWhenDescriptionValidationIsCorrect() {
        //Делаем описание фильма некорректным и проверяем валидацию.
        film.setDescription(" ");
        assertThrows(ValidationException.class, () -> inMemoryFilmStorage.create(film),
                "Не выброшено исключение ValidationException");

        film.setDescription(null);
        assertThrows(ValidationException.class, () -> inMemoryFilmStorage.create(film),
                "Не выброшено исключение ValidationException");

        film.setDescription(stringGenerate(201));
        assertThrows(ValidationException.class, () -> inMemoryFilmStorage.create(film),
                "Не выброшено исключение ValidationException");

        assertEquals(0, inMemoryFilmStorage.getAll().size(), "Фильм не должен быть создан");

        //Создаем фильм с максимально возможным количеством символов в описании.
        film.setDescription(stringGenerate(200));
        inMemoryFilmStorage.create(film);
        assertEquals(1, inMemoryFilmStorage.getAll().size(), "Фильм должен быть создан");
    } //Проверяем корректность работы валидации на описание фильма

    @Test
    public void shouldReturnPositiveWhenReleaseDateValidationIsCorrect() {
        film.setReleaseDate(null);
        assertThrows(ValidationException.class, () -> inMemoryFilmStorage.create(film),
                "Не выброшено исключение ValidationException");

        //Делаем дату выхода фильма некорректной и проверяем валидацию.
        film.setReleaseDate(LocalDate.from(LocalDate.of(1895, 12, 27)));
        assertThrows(ValidationException.class, () -> inMemoryFilmStorage.create(film),
                "Не выброшено исключение ValidationException");

        assertEquals(0, inMemoryFilmStorage.getAll().size(), "Фильм не должен быть создан");

        //Проверяем корректность самой ранней даты выхода фильма
        film.setReleaseDate(LocalDate.from(LocalDate.of(1895, 12, 28)));
        inMemoryFilmStorage.create(film);
        assertEquals(1, inMemoryFilmStorage.getAll().size(), "Фильм должен быть создан");
    } //Проверяем корректность работы валидации на дату релиза фильма

    @Test
    public void shouldReturnPositiveWhenDurationValidationIsCorrect() {
        //Делаем длительность фильма некорректной и проверяем валидацию.
        film.setDuration(-1);
        assertThrows(ValidationException.class, () -> inMemoryFilmStorage.create(film),
                "Не выброшено исключение ValidationException");

        film.setDuration(0);
        assertThrows(ValidationException.class, () -> inMemoryFilmStorage.create(film),
                "Не выброшено исключение ValidationException");

        //Проверяем корректность с минимальной длительностью фильма
        film.setDuration(1);
        inMemoryFilmStorage.create(film);
        assertEquals(1, inMemoryFilmStorage.getAll().size(), "Фильм должен быть создан");
    } //Проверяем корректность работы валидации на длительность фильма

    private String stringGenerate(int desiredLength) {
        StringBuilder sb = new StringBuilder(desiredLength);

        while (sb.length() < desiredLength) {
            sb.append("a");
        }

        return sb.toString();
    }

    @Test
    public void shouldReturnPositiveWhenGetAllIsCorrect() {
        //Проверяем что фильмы еще не добавлялись.
        assertEquals(0, inMemoryFilmStorage.getAll().size(), "Не должно быть фильмов");

        //Добавляем фильм
        inMemoryFilmStorage.create(film);

        //Проверяем, что в ответе метода есть добавленный фильм
        assertEquals(1, inMemoryFilmStorage.getAll().size(), "Должен быть 1 фильм");
    }

    @Test
    public void shouldReturnPositiveWhenAddLikeIsCorrect() {


        Film createdFilm = inMemoryFilmStorage.create(film);
        User cratedUser = new User();

        cratedUser.setEmail("blabla@gmail.com");
        cratedUser.setLogin("bobo");
        cratedUser.setName("Boris");
        cratedUser.setBirthday(LocalDate.of(2000, 5, 5));


        User user = inMemoryUserStorage.create(cratedUser);
        inMemoryFilmStorage.addLike(createdFilm.getId(), user.getId());

        Optional<Film> filmOptional = inMemoryFilmStorage.getAll().stream().findFirst();
        Set<Long> likes = new HashSet<>();

        if (filmOptional.isPresent()) {
            likes = filmOptional.get().getLikes();

        }

        assertTrue(likes.contains(user.getId()), "Должен быть лайк от пользователя с id " + user.getId());

    } //Проверяем корректность добавления лайка

    @Test
    public void shouldReturnPositiveWhenRemoveLikeIsCorrect() {
        //Удаляем лайк
        Film createdFilm = inMemoryFilmStorage.create(film);
        User cratedUser = new User();

        cratedUser.setEmail("blabla@gmail.com");
        cratedUser.setLogin("bobo");
        cratedUser.setName("Boris");
        cratedUser.setBirthday(LocalDate.of(2000, 5, 5));


        User user = inMemoryUserStorage.create(cratedUser);
        inMemoryFilmStorage.addLike(createdFilm.getId(), user.getId());
        inMemoryFilmStorage.removeLike(createdFilm.getId(), user.getId());

        Optional<Film> filmOptional = inMemoryFilmStorage.getAll().stream().findFirst();
        Set<Long> likes = new HashSet<>();

        if (filmOptional.isPresent()) {
            likes = filmOptional.get().getLikes();

        }

        assertTrue(likes.isEmpty(), "Не должно быть лайков");

    } //Проверяем корректность удаления лайка

    @Test
    public void shouldReturnPositiveWhenGetAllRatingIsCorrect() {
        Collection<Mpa> allRatings = inMemoryFilmStorage.getAllRatings();
        final Integer MAX_RATING = 5;
        assertEquals(MAX_RATING, allRatings.size());
    } //Проверяем корректность получения всех рейтингов

    @Test
    public void shouldReturnPositiveWhenGetRatingIsCorrect() {

        Mpa mpa = new Mpa();
        mpa.setName("G");
        mpa.setId(1);

        Mpa mpaDtoBd = inMemoryFilmStorage.getRatingBiId(1);

        assertEquals(mpa, mpaDtoBd, "Объекты должны быть равны");
    } //Проверяем корректность получения конкретного рейтинга

    @Test
    public void shouldReturnPositiveWhenGetAllGenresIsCorrect() {

        Collection<Genre> allGenres = inMemoryFilmStorage.getAllGenres();
        final Integer MAX_GENRE = 6;
        assertEquals(MAX_GENRE, allGenres.size());
    } //Проверяем корректность получения всех жанров

    @Test
    public void shouldReturnPositiveWhenGetGenreIsCorrect() {
        Genre genre = new Genre();
        genre.setName("Комедия");
        genre.setId(1);

        Genre genreBd = inMemoryFilmStorage.getGenreBiId(1);

        assertEquals(genre, genreBd, "Объекты должны быть равны");

    } //Проверяем корректность получения конкретного жанра


}
