package ru.eventorg.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.UserNotEventParticipantException;
import ru.eventorg.repository.EventUserListEntityRepository;

@Service
public class ParticipantValidationService {
    private final EventUserListEntityRepository eventUserListEntityRepository;

    public ParticipantValidationService(EventUserListEntityRepository eventUserListEntityRepository) {
        this.eventUserListEntityRepository = eventUserListEntityRepository;
    }

    public Mono<Void> validateIsParticipant(Integer eventId, String userLogin) {
        return eventUserListEntityRepository.existsEventIdByEventIdAndUserId(eventId, userLogin)
                .flatMap(isParticipant -> isParticipant
                        ? Mono.empty()
                        : Mono.error(new UserNotEventParticipantException(ErrorState.USER_NOT_EVENT_PARTICIPANT)));
    }
}