package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dto.MpaDto;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;

@RequiredArgsConstructor
@RestController
@RequestMapping("/mpa")
public class MpaController {
    private final FilmService filmService;

    @GetMapping
    public Collection<MpaDto> getAllRatings() {
        return filmService.getAllMpa();
    }

    @GetMapping("/{id}")
    public MpaDto getRatingBiID(@PathVariable Integer id) {
        return filmService.getMpa(id);
    }
}
