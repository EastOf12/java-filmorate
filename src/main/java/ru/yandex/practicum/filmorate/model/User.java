package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {
    private Long id;
    private String email;
    private String login;
    private String name;
    private Set<Long> friends = new HashSet<>(); //Айди друзей пользователя

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
}
