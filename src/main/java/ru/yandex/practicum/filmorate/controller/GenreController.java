package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;

@RequiredArgsConstructor
@RestController
@RequestMapping("/genres")
public class GenreController {
    private final FilmService filmService;

    @GetMapping
    public Collection<GenreDto> getAllGenres() {
        return filmService.getAllGenres();
    }

    @GetMapping("/{id}")
    public GenreDto getGenreBiID(@PathVariable Integer id) {
        return filmService.getGenre(id);
    }
}
