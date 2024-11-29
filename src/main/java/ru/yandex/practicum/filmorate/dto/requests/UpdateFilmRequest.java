package ru.yandex.practicum.filmorate.dto.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import ru.yandex.practicum.filmorate.model.Genre;

import java.time.LocalDate;
import java.util.Collection;

@Data
public class UpdateFilmRequest {
    private Long id;
    private String name;
    private String description;
    private Integer duration;
    private Integer rating;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;
    private Collection<Genre> genres;
}
