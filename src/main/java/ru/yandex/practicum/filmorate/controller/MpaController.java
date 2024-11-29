package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dto.MpaDto;
import ru.yandex.practicum.filmorate.service.MpaService;

import java.util.Collection;

@RequiredArgsConstructor
@RestController
@RequestMapping("/mpa")
public class MpaController {
    private final MpaService mpaService;

    @GetMapping
    public Collection<MpaDto> getAllMpa() {
        return mpaService.getAllMpa();
    }

    @GetMapping("/{id}")
    public MpaDto getMpaBiID(@PathVariable Integer id) {
        return mpaService.getMpa(id);
    }
}
