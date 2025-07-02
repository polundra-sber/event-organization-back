package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import ru.eventorg.entity.EventUserList;

public interface EventUserListRepository extends R2dbcRepository<EventUserList, String> {
    Flux<EventUserList> findEventUserListByUserId(String userId);
}
