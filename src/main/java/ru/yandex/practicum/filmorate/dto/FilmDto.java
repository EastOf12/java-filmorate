package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

@Data
public class FilmDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private long id;
    private String name;
    private String description;
    private int duration;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;
    private Set<Long> likes; //Айди друзей пользователя
    private Mpa mpa;
    private Collection<Genre> genres; //Айди жанров фильма
}
