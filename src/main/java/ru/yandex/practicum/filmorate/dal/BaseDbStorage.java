package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class BaseDbStorage<T> {
    protected final JdbcTemplate jdbcTemplate;
    protected final RowMapper<T> mapper;
    private final Class<T> entityType;

    //Сохраняет данные в таблице и возвращает сгенерированный Id
    protected Long insertGetId(String query, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                // Устанавливаем параметры
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                return ps;
            }, keyHolder);

            // Возвращаем сгенерированный ID
            return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;

        } catch (DataAccessException e) {
            log.error("Ошибка при добавлении данных: {}", e.getMessage());
            throw new RuntimeException("Ошибка при сохранении в БД " + e.getMessage());
        }
    } //Вставляем данные в таблицу

    //Сохраняет данные в таблице без генерации id
    protected void insert(String query, Object... params) {

        try {
            jdbcTemplate.update(
                    query,
                    params
            );

        } catch (DataAccessException e) {
            log.error("Ошибка при добавлении данных: {}", e.getMessage());
            throw new RuntimeException("Ошибка при сохранении в БД " + e.getMessage());
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
