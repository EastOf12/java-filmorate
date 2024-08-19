package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDate;

@Data
public class Film {
    Long id;
    String name;
    String description;
    int duration;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate releaseDate;
}
