package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.RoleEntity;

import java.util.List;

public interface RoleEntityRepository extends R2dbcRepository<RoleEntity, Integer> {
    Mono<RoleEntity> getRoleEntityByRoleId(Integer roleId);
    Mono<Integer> getRoleIdByRoleName(String roleName);
}
