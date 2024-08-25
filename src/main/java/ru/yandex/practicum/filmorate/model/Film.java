package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Film {
    private Long id;
    private String name;
    private String description;
    private int duration;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;
}
