package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.requests.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.requests.UpdateFilmRequest;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;

@RequiredArgsConstructor
@RestController
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FilmDto create(@Valid @RequestBody NewFilmRequest newFilmRequest) {
        return filmService.create(newFilmRequest);
    }

    @PutMapping
    public FilmDto update(@RequestBody UpdateFilmRequest updateFilmRequest) {
        return filmService.update(updateFilmRequest);
    }

    //Получение конкретного фильма
    @GetMapping("/{id}")
    public FilmDto getGenreBiID(@PathVariable Long id) {
        return filmService.getFilm(id);
    }

    @GetMapping
    public Collection<FilmDto> getAll() {
        return filmService.getAll();
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void deleteLike(@PathVariable Long id, @PathVariable Long userId) {
        filmService.deleteLike(id, userId);
    }

    @GetMapping("/popular")
    public Collection<FilmDto> getPopularFilms(@RequestParam(defaultValue = "10") int count) {
        return filmService.getPopularFilms(count);
    }
}
