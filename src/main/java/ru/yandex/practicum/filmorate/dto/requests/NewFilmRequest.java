package ru.yandex.practicum.filmorate.dto.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.Collection;

@Data
public class NewFilmRequest {
    private String name;
    private String description;
    private Integer duration;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;
    private Mpa mpa;
    private Collection<Genre> genres;
}
