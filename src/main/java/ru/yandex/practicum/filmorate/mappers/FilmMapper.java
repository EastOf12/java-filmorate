package ru.yandex.practicum.filmorate.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.requests.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.requests.UpdateFilmRequest;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilmMapper {


    public static Film mapToFilm(NewFilmRequest request) {
        Film film = new Film();
        film.setName(request.getName());
        film.setDescription(request.getDescription());
        film.setDuration(request.getDuration());
        film.setReleaseDate(request.getReleaseDate());

        if (request.getMpa() == null) {
            Mpa mpa = new Mpa();
            mpa.setId(1);
            mpa.setName("G");
            film.setMpa(mpa);
        } else {
            film.setMpa(request.getMpa());
        }

        if (request.getGenres() == null) {
            Collection<Genre> genres = new HashSet<>();
            film.setGenres(genres);
        } else {
            film.setGenres(request.getGenres());
        }
        return film;
    }


    public static FilmDto mapToFilmDto(Film film) {
        FilmDto dto = new FilmDto();

        dto.setId(film.getId());
        dto.setName(film.getName());
        dto.setDescription(film.getDescription());
        dto.setDuration(film.getDuration());
        dto.setReleaseDate(film.getReleaseDate());
        dto.setMpa(film.getMpa());


        if (Objects.nonNull(film.getLikes())) {
            Set<Long> likesIds = new HashSet<>(film.getLikes());
            dto.setLikes(likesIds);
        }

        if (Objects.nonNull(film.getGenres())) {
            dto.setGenres(film.getGenres());
        }

        return dto;
    }


    //Доработать обновление
    public static Film mapToFilmUpdate(UpdateFilmRequest request) {
        Film film = new Film();

        if (request.getId() != null) {
            film.setId(request.getId());
        }
        if (request.getName() != null) {
            film.setName(request.getName());
        }
        if (request.getDescription() != null) {
            film.setDescription(request.getDescription());
        }
        if (request.getDuration() != null) {
            film.setDuration(request.getDuration());
        }
        if (request.getReleaseDate() != null) {
            film.setReleaseDate(request.getReleaseDate());
        }

        if (Objects.nonNull(film.getGenres())) {
            film.setGenres(request.getGenres());
        }

        return film;
    }
}
