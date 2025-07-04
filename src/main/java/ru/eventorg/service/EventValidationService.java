package ru.eventorg.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.exception.EventNotExistException;
import ru.eventorg.repository.EventEntityRepository;

@Service
public class EventValidationService {
    private final EventEntityRepository eventRepository;

    public EventValidationService(EventEntityRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Mono<Void> validateExists(Integer eventId) {
        return eventRepository.existsById(eventId)
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : Mono.error(new EventNotExistException()));
    }
}