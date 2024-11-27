package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.GenreDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;


@Service
@Slf4j
@Component
@RequiredArgsConstructor
public class InMemoryGenreStorage implements GenreStorage {
    private static final int MAX_GENRE = 6;
    private final GenreDbStorage genreDbStorage;

    @Override
    public Collection<Genre> getAllGenres() {
        return genreDbStorage.getAllGenres();
    }

    @Override
    public Genre getGenreBiId(Integer genreId) {

        if (MAX_GENRE < genreId) {
            throw new NotFoundException("Такой жанр не существует");
        }

        return genreDbStorage.getGenre(genreId);
    }
}

