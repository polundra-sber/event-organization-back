package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.openapitools.model.EventEditor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.Event;
import ru.eventorg.entity.EventStatus;
import ru.eventorg.entity.EventUserList;
import ru.eventorg.model.EventEditor;
import ru.eventorg.model.EventNotExistResponse;
import ru.eventorg.model.EventPreview;
import ru.eventorg.repository.EventRepository;
import ru.eventorg.repository.EventStatusRepository;
import ru.eventorg.repository.EventUserListRepository;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class HomePageService {

    private final EventRepository eventRepository;
    private final EventStatusRepository eventStatusRepository;
    private final EventUserListRepository eventUserListRepository;

    public Mono<Void> completeEvent(Integer eventId) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Event not found")))
                .flatMap(event -> {
                    event.setStatusId(3); // Assuming 3 is "completed" status
                    return eventRepository.save(event);
                })
                .then();
    }

    public Mono<Event> createEvent(EventEditor event) {
        event.setStatusId(1); // Assuming 1 is "active" status
        return eventRepository.save(event)
                .map(savedEvent -> new EventEditor(
                        savedEvent.getEventName(),
                        savedEvent.getEventDescription(),
                        savedEvent.getLocation(),
                        savedEvent.getEventDate(),
                        savedEvent.getEventTime(),
                        savedEvent.getChatLink()
                ));
    }

    public Mono<Void> deleteEvent(Integer eventId) {
        return eventRepository.deleteById(eventId)
                .onErrorResume(e -> Mono.error(new IllegalStateException("Failed to delete event")));
    }

    public Mono<EventEditor> editEvent(Integer eventId, EventEditor editor) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Event not found")))
                .flatMap(event -> {
                    event.setEventName(editor.getEventName());
                    event.setEventDescription(editor.getEventDescription());
                    event.setLocation(editor.getLocation());
                    event.setEventDate(editor.getEventDate());
                    event.setEventTime(editor.getEventTime());
                    event.setChatLink(editor.getChatLink());
                    return eventRepository.save(event);
                })
                .map(updatedEvent -> editor);
    }

    public Mono<EventPreview> findEventById(Integer eventId) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Event not found")))
                .zipWith(eventStatusRepository.findById(event.getStatusId()))
                .map(tuple -> {
                    Event event = tuple.getT1();
                    EventStatus status = tuple.getT2();
                    return new EventPreview(
                            event.getEventId(),
                            event.getEventName(),
                            event.getLocation(),
                            event.getEventDate(),
                            event.getEventTime(),
                            status.getEventStatusName()
                    );
                });
    }

    public Mono<Event> getEventById(Integer eventId) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Event not found")));
    }

    public Flux<Event> getEvents() {
        // In a real application, you would filter by current user
        return eventRepository.findAll();
    }

    public Mono<Void> leaveEvent(Integer eventId) {
        // In a real application, you would get current user ID
        String currentUserId = "current-user-id";
        return eventUserListRepository.findByEventIdAndUserId(eventId, currentUserId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User is not part of this event"))))
                .flatMap(eventUserList -> eventUserListRepository.deleteById(eventUserList.getEventUserListId()))
                .then();
    }

    public Mono<String> sendJoinEventRequest(Integer eventId) {
        // In a real application, you would get current user ID
        String currentUserId = "current-user-id";

        return eventRepository.existsById(eventId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException("Event not found"));
                    }
                    return eventUserListRepository.existsByEventIdAndUserId(eventId, currentUserId);
                })
                .flatMap(alreadyJoined -> {
                    if (alreadyJoined) {
                        return Mono.error(new IllegalStateException("You are already part of this event"));
                    }

                    EventUserList newJoin = new EventUserList();
                    newJoin.setEventId(eventId);
                    newJoin.setUserId(currentUserId);
                    newJoin.setRoleId(2); // Assuming 2 is "participant" role

                    return eventUserListRepository.save(newJoin)
                            .thenReturn("Join request sent successfully");
                });
    }
}