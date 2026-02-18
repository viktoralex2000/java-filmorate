package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.Set;

@Data
public class FilmRequestDto {
    private long id;

    @NotBlank(message = "Название фильма не может быть пустым.")
    private String name;

    @Size(max = 200, message = "Описание не может превышать 200 символов.")
    private String description;

    @NotNull(message = "Дата релиза обязательна.")
    private LocalDate releaseDate;

    @AssertTrue(message = "Дата релиза не может быть раньше 28.12.1895")
    public boolean isReleaseDateValid() {
        return releaseDate == null
                || !releaseDate.isBefore(LocalDate.of(1895, 12, 28));
    }

    @Positive(message = "Продолжительность фильма должна быть положительным числом.")
    private int duration;

    private Mpa mpa;

    private Set<Genre> genres;
}
