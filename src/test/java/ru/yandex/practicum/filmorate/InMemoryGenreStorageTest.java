package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.GenreDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.InMemoryGenreStorage;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({GenreDbStorage.class, InMemoryGenreStorage.class, FilmRowMapper.class})
public class InMemoryGenreStorageTest {
    private final InMemoryGenreStorage inMemoryGenreStorage;

    @Test
    public void shouldReturnPositiveWhenGetAllGenresIsCorrect() {

        Collection<Genre> allGenres = inMemoryGenreStorage.getAllGenres();
        final Integer MAX_GENRE = 6;
        assertEquals(MAX_GENRE, allGenres.size());
    } //Проверяем корректность получения всех жанров

    @Test
    public void shouldReturnPositiveWhenGetGenreIsCorrect() {
        Genre genre = new Genre();
        genre.setName("Комедия");
        genre.setId(1);

        Genre genreBd = inMemoryGenreStorage.getGenreBiId(1);

        assertEquals(genre, genreBd, "Объекты должны быть равны");

    } //Проверяем корректность получения конкретного жанра
}
