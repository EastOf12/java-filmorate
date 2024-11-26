package ru.yandex.practicum.filmorate.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.MpaDto;
import ru.yandex.practicum.filmorate.model.Mpa;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MpaMapper {

    public static Mpa mapToMpa(MpaDto mpaDto) {
        Mpa mpa = new Mpa();

        mpa.setId(mpaDto.getId());
        mpa.setName(mpaDto.getName());
        return mpa;
    }


    public static MpaDto mapToMpaDto(Mpa mpa) {
        MpaDto mpaDto = new MpaDto();

        mpaDto.setId(mpa.getId());
        mpaDto.setName(mpa.getName());
        return mpaDto;
    }

}
