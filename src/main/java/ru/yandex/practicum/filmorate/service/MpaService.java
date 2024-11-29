package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.MpaDbStorage;
import ru.yandex.practicum.filmorate.dto.MpaDto;
import ru.yandex.practicum.filmorate.mappers.MpaMapper;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class MpaService {
    private final MpaDbStorage mpaDbStorage;


    public MpaService(MpaDbStorage mpaDbStorage) {
        this.mpaDbStorage = mpaDbStorage;
    }

    public Collection<MpaDto> getAllMpa() {
        List<MpaDto> mpaDtos = new ArrayList<>();
        Collection<Mpa> mpas = mpaDbStorage.getAllMpa();

        for (Mpa mpa : mpas) {
            MpaDto mpaDto = new MpaDto();
            mpaDto.setId(mpa.getId());
            mpaDto.setName(mpa.getName());
            mpaDtos.add(mpaDto);
        }

        mpaDtos.sort((g1, g2) -> Integer.compare(g1.getId(), g2.getId()));

        return mpaDtos;
    }

    public MpaDto getMpa(int id) {
        return MpaMapper.mapToMpaDto(mpaDbStorage.getMpa(id));
    }

}
