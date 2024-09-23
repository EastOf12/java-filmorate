package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class Film {
    private Long id;
    private String name;
    private String description;
    private int duration;
    private Set<Long> likes = new HashSet<>(); //Айди пользователей, который лайкнули фильм

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;
}
