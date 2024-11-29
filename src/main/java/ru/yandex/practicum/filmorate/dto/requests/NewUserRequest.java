package ru.yandex.practicum.filmorate.dto.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class NewUserRequest {

    private String name;
    private String login;
    private String email;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
}
