package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.eventorg.entity.RoleEntity;

public interface RoleEntityRepository extends R2dbcRepository<RoleEntity, Integer> {
}
