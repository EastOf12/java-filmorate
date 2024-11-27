package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.MpaDbStorage;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.Collection;

@Service
@Slf4j
@Component
@RequiredArgsConstructor
public class InMemoryMpaStorage implements MpaStorage {
    private final MpaDbStorage mpaDbStorage;

    @Override
    public Collection<Mpa> getAllMpa() {
        return mpaDbStorage.getAllMpa();
    }

    @Override
    public Mpa getMpaBiId(Integer ratingId) {
        return mpaDbStorage.getMpa(ratingId);
    }
}
