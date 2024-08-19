package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Data
@Service
public class User {
    Long id;
    String email;
    String login;
    String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate birthday;
}
