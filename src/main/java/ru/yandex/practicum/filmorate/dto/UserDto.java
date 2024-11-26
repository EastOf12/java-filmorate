package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private long id;
    private String name;
    private String email;
    private String login;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    private Set<Long> friends; //Айди друзей пользователя
}
