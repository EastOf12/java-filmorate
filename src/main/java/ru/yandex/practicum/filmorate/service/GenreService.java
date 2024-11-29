package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.GenreDbStorage;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.mappers.GenreMapper;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class GenreService {
    private final GenreDbStorage genreDbStorage;

    public GenreService(GenreDbStorage genreDbStorage) {
        this.genreDbStorage = genreDbStorage;
    }

    public Collection<GenreDto> getAllGenres() {
        Collection<Genre> genres = genreDbStorage.getAllGenres();
        List<GenreDto> genreDtos = new ArrayList<>();

        // Заполняем список genreDtos из коллекции жанров
        for (Genre genre : genres) {
            GenreDto genreDto = new GenreDto();
            genreDto.setId(genre.getId());
            genreDto.setName(genre.getName());
            genreDtos.add(genreDto); // Добавляем genreDto в список
        }

        return genreDtos;
    }

    public GenreDto getGenre(int id) {
        return GenreMapper.mapToGenreDto(genreDbStorage.getGenre(id));
    }
}
