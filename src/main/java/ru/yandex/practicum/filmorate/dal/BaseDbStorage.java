package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class BaseDbStorage<T> {
    protected final JdbcTemplate jdbcTemplate;
    protected final RowMapper<T> mapper;
    private final Class<T> entityType;

    //Сохраняет данные в таблице
    protected void insert(String query, Object... params) {

        try {
            jdbcTemplate.update(
                    query,
                    params
            );

        } catch (DataAccessException e) {
            log.error("Ошибка при добавлении данных: {}", e.getMessage());
        }
    } //Вставляем данные в таблицу

    protected Optional<T> find(String query, Object... params) {
        try {
            T result = jdbcTemplate.queryForObject(query, mapper, params);
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    } //Получаем отдельную запись из таблицы

    protected List<T> findAll(String query) {
        return jdbcTemplate.query(query, mapper);
    } //Получаем все данные из таблицы
}
