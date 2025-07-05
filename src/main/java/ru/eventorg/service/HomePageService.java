package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.openapitools.model.EventEditor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.EventCustom;
import ru.eventorg.dto.EventPreviewCustom;
import ru.eventorg.entity.EventEntity;
import ru.eventorg.entity.EventStatusEntity;
import ru.eventorg.entity.EventUserListEntity;
import ru.eventorg.entity.RoleEntity;
import ru.eventorg.exception.*;
import ru.eventorg.repository.EventEntityRepository;
import ru.eventorg.repository.EventStatusEntityRepository;
import ru.eventorg.repository.EventUserListEntityRepository;
import ru.eventorg.repository.RoleEntityRepository;
import ru.eventorg.security.SecurityUtils;
import ru.eventorg.service.enums.EventStatus;
import ru.eventorg.service.enums.UserRole;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HomePageService {

    private final EventEntityRepository eventRepository;
    private final EventStatusEntityRepository eventStatusRepository;
    private final RoleEntityRepository roleRepository;
    private final EventUserListEntityRepository eventUserListRepository;
    private final RoleService roleService;
    private final R2dbcEntityTemplate template;
    private final EventService eventService;


    public Mono<Void> completeEvent(Integer eventId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        Mono.zip(
                                        roleService.checkIfCreator(eventId, currentUserLogin),
                                        eventService.getEventStatus(eventId)
                                )
                                .flatMap(tuple -> {
                                    boolean isCreator = tuple.getT1();
                                    String statusName = tuple.getT2();

                                    if (!isCreator) {
                                        return Mono.error(new WrongUserRoleException(ErrorState.NOT_CREATOR_ROLE));
                                    }
                                    if (!EventStatus.ACTIVE.getDisplayName().equalsIgnoreCase(statusName)) {
                                        return Mono.error(new EventNotActiveException(ErrorState.EVENT_NOT_ACTIVE));
                                    }
                                    return updateEventStatus(eventId, 2);
                                })
                );
    }

    public Mono<EventCustom> createEvent(EventEditor event) {
        EventEntity eventEntity = createEventEntityFromCreator(event);

        return eventRepository.save(eventEntity)
                .flatMap(this::createEventUserListAndBuildCustom);
    }

    //проверка статуса мероприятия
    public Mono<Void> deleteEvent(Integer eventId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        Mono.zip(
                                        roleService.checkIfCreator(eventId, currentUserLogin),
                                        eventService.getEventStatus(eventId)
                                )
                                .flatMap(tuple -> {
                                    boolean isCreator = tuple.getT1();
                                    String statusName = tuple.getT2();

                                    if (!isCreator) {
                                        return Mono.error(new WrongUserRoleException(ErrorState.NOT_CREATOR_ROLE));
                                    }
                                    if (!EventStatus.ACTIVE.getDisplayName().equalsIgnoreCase(statusName)) {
                                        return Mono.error(new EventNotActiveException(ErrorState.EVENT_NOT_ACTIVE));
                                    }
                                    return updateEventStatus(eventId, 3);
                                })
                );
    }

    //здесь нужно проверять роль пользователя
    // проверка, что меро существует, что я его участник
    public Mono<EventEntity> editEvent(Integer eventId, EventEditor editor) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        Mono.zip(
                                        roleService.checkIfCreator(eventId, currentUserLogin),
                                        eventService.getEventStatus(eventId),
                                        eventRepository.getEventByEventId(eventId)
                                )
                                .flatMap(tuple -> {
                                    Boolean isCreator = tuple.getT1();
                                    String statusName = tuple.getT2();
                                    EventEntity event = tuple.getT3();

                                    // 1. Проверка что пользователь - создатель
                                    if (!isCreator) {
                                        return Mono.error(new WrongUserRoleException(ErrorState.NOT_CREATOR_ROLE));
                                    }

                                    // 2. Проверка что мероприятие активно
                                    if (!EventStatus.ACTIVE.getDisplayName().equalsIgnoreCase(statusName)) {
                                        return Mono.error(new EventNotActiveException(ErrorState.EVENT_NOT_ACTIVE));
                                    }

                                    // 3. Обновление и сохранение
                                    EventEntity updatedEvent = updateEventFromEditor(event, editor);
                                    return eventRepository.save(updatedEvent);
                                })
                );
    }

    public Mono<EventPreviewCustom> findEventById(Integer eventId) {
        return eventRepository.getActiveOrCompletedEventById(eventId)
                .switchIfEmpty(Mono.error(new EventNotExistException(ErrorState.EVENT_NOT_EXIST)))
                .flatMap(this::buildEventPreviewCustom);
    }

    //проверить работу
    public Mono<EventCustom> getEventById(Integer eventId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username ->
                        Mono.zip(
                                roleService.getUserRoleInEvent(eventId, username),
                                eventService.getEventStatus(eventId),
                                Mono.just(username) // Сохраняем username для дальнейшего использования
                        )
                )
                .flatMap(tuple -> {
                    String roleName = tuple.getT1();
                    String statusName = tuple.getT2();
                    String username = tuple.getT3(); // Получаем сохраненное имя пользователя

                    // Проверка роли
                    if (UserRole.NOT_ALLOWED.getDisplayName().equalsIgnoreCase(roleName)) {
                        return Mono.error(new UserNotEventParticipantException(ErrorState.USER_NOT_EVENT_PARTICIPANT));
                    }

                    // Проверка статуса мероприятия
                    if (!EventStatus.ACTIVE.getDisplayName().equalsIgnoreCase(statusName) &&
                            !EventStatus.COMPLETED.getDisplayName().equalsIgnoreCase(statusName)) {
                        return Mono.error(new EventNotActiveException(ErrorState.EVENT_NOT_ACTIVE));
                    }
                    String query = """
                    SELECT 
                        e.event_id,
                        e.event_name,
                        e.event_description,
                        e.location,
                        e.event_date,
                        e.event_time,
                        e.chat_link,
                        es.event_status_name,
                        r.role_name
                    FROM event e
                    JOIN event_status es ON e.status_id = es.event_status_id
                    JOIN event_user_list eul ON e.event_id = eul.event_id
                    JOIN role r ON eul.role_id = r.role_id
                    WHERE e.event_id = :eventId
                    AND eul.user_id = :userId
                    """;
                    return template.getDatabaseClient()
                            .sql(query)
                            .bind("eventId", eventId)
                            .bind("userId", username)
                            .map(this::mapToEventCustom)
                            .one();
                });
    }


    public Flux<EventCustom> getEvents() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMapMany(username -> {
                    String query = """
                    
                            SELECT e.*,
                                   es.event_status_name,
                                   r.role_name
                    FROM event_user_list eul
                    JOIN event e ON eul.event_id = e.event_id
                    JOIN event_status es ON e.status_id = es.event_status_id
                    JOIN role r ON eul.role_id = r.role_id
                    WHERE eul.user_id = :userId
                    AND r.role_name != 'Не допущен'
                    AND e.status_id in (1,2)
                    """;

                    return template.getDatabaseClient()
                            .sql(query)
                            .bind("userId", username)
                            .map(this::mapToEventCustom)
                            .all();
                });
    }

    //пользователь - участник
    public Mono<Void> leaveEvent(Integer eventId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userId ->
                        // 1. Проверяем что мероприятие активно
                        eventRepository.existsActiveEventById(eventId)
                                .flatMap(isActive -> {
                                    if (!isActive) {
                                        return Mono.error(new EventNotActiveException(ErrorState.EVENT_NOT_ACTIVE));
                                    }

                                    // 2. Проверяем роль пользователя
                                    return roleService.getUserRoleInEvent(eventId, userId)
                                            .flatMap(roleName -> {
                                                // Проверяем, что пользователь не создатель
                                                if (UserRole.CREATOR.getDisplayName().equalsIgnoreCase(roleName)) {
                                                    return Mono.error(new WrongUserRoleException(ErrorState.CREATOR_CANNOT_LEAVE));
                                                }
                                                // Проверяем, что пользователь не "Не допущен"
                                                if (UserRole.NOT_ALLOWED.getDisplayName().equalsIgnoreCase(roleName)) {
                                                    return Mono.error(new UserNotEventParticipantException(ErrorState.USER_NOT_EVENT_PARTICIPANT));
                                                }
                                                // Если все проверки пройдены - удаляем запись
                                                return eventUserListRepository.deleteEventUserListEntityByEventIdAndUserId(eventId, userId)
                                                        .then();
                                            });
                                })
                );
    }

    public Mono<String> sendJoinEventRequest(Integer eventId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(currentUserId -> checkAndJoinEvent(eventId, currentUserId));
    }

    // Private helper methods
    private Mono<Void> updateEventStatus(Integer eventId, Integer statusId) {
        return eventRepository.getEventByEventId(eventId)
                .switchIfEmpty(Mono.error(new EventNotExistException(ErrorState.EVENT_NOT_EXIST)))
                .flatMap(event -> {
                    event.setStatusId(statusId);
                    return eventRepository.save(event);
                })
                .then();
    }

    private EventCustom mapToEventCustom(io.r2dbc.spi.Readable readable) {
        EventCustom event = new EventCustom();
        event.setEventId(readable.get("event_id", Integer.class));
        event.setEventName(readable.get("event_name", String.class));
        event.setEventDescription(readable.get("event_description", String.class));
        event.setLocation(readable.get("location", String.class));
        event.setEventDate(readable.get("event_date", LocalDate.class));
        event.setEventTime(Objects.requireNonNull(readable.get("event_time", LocalTime.class)));
        event.setChatLink(readable.get("chat_link", String.class));
        event.setStatus(readable.get("event_status_name", String.class));
        event.setRole(readable.get("role_name", String.class));
        return event;
    }

    private EventEntity createEventEntityFromCreator(EventEditor event) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setStatusId(1);
        eventEntity.setCostAllocated(false);
        eventEntity.setEventName(event.getEventName());
        eventEntity.setEventDescription(event.getEventDescription());
        eventEntity.setLocation(event.getLocation());
        eventEntity.setEventDate(event.getEventDate());
        eventEntity.setEventTime(LocalTime.parse(event.getEventTime()));
        eventEntity.setChatLink(event.getChatLink());
        return eventEntity;
    }

    private Mono<EventCustom> createEventUserListAndBuildCustom(EventEntity savedEvent) {
        Mono<EventStatusEntity> statusMono = eventStatusRepository.getEventStatusByEventStatusId(savedEvent.getStatusId());
        Mono<RoleEntity> roleMono = roleRepository.getRoleEntityByRoleId(3); // 3 - ID роли организатора

        return Mono.zip(statusMono, roleMono)
                .flatMap(tuple -> SecurityUtils.getCurrentUserLogin()
                        .flatMap(username -> {
                            EventUserListEntity eventUserListEntity = createEventUserList(savedEvent, tuple.getT2(), username);
                            EventCustom eventCustom = buildEventCustom(savedEvent, tuple.getT1(), tuple.getT2());

                            return eventUserListRepository.save(eventUserListEntity)
                                    .thenReturn(eventCustom);
                        }));
    }

    private EventUserListEntity createEventUserList(EventEntity event, RoleEntity role, String username) {
        EventUserListEntity entity = new EventUserListEntity();
        entity.setEventId(event.getEventId());
        entity.setRoleId(role.getRoleId());
        entity.setUserId(username);
        return entity;
    }

    private EventCustom buildEventCustom(EventEntity event, EventStatusEntity status, RoleEntity role) {
        EventCustom eventCustom = new EventCustom();
        eventCustom.setEventId(event.getEventId());
        eventCustom.setEventName(event.getEventName());
        eventCustom.setEventDescription(event.getEventDescription());
        eventCustom.setStatus(status.getEventStatusName());
        eventCustom.setLocation(event.getLocation());
        eventCustom.setEventDate(event.getEventDate());
        eventCustom.setEventTime(event.getEventTime());
        eventCustom.setChatLink(event.getChatLink());
        eventCustom.setRole(role.getRoleName());
        return eventCustom;
    }

    private EventEntity updateEventFromEditor(EventEntity event, EventEditor editor) {
        event.setEventName(editor.getEventName());
        event.setEventDescription(editor.getEventDescription());
        event.setLocation(editor.getLocation());
        event.setEventDate(editor.getEventDate());
        event.setEventTime(LocalTime.parse(editor.getEventTime()));
        event.setChatLink(editor.getChatLink());
        return event;
    }

    private Mono<EventPreviewCustom> buildEventPreviewCustom(EventEntity event) {
        return eventStatusRepository.getEventStatusByEventStatusId(event.getStatusId())
                .map(status -> new EventPreviewCustom(
                        event.getEventId(),
                        event.getEventName(),
                        status.getEventStatusName(),
                        event.getLocation(),
                        event.getEventDate(),
                        event.getEventTime()
                ));
    }

    private Mono<String> checkAndJoinEvent(Integer eventId, String userId) {
        return eventUserListRepository.existsEventIdByEventIdAndUserId(eventId, userId)
                .flatMap(alreadyJoined -> {
                    if (alreadyJoined) {
                        return Mono.error(new UserAlreadyJoinException(ErrorState.USER_ALREADY_JOINED));
                    }

                    // Проверяем существование мероприятия и его статус
                    return eventRepository.getEventByEventId(eventId)
                            .switchIfEmpty(Mono.error(new EventNotExistException(ErrorState.EVENT_NOT_EXIST)))
                            .flatMap(event -> eventStatusRepository.getEventStatusByEventStatusId(event.getStatusId())
                                    .flatMap(status -> {
                                        if (!EventStatus.ACTIVE.getDisplayName().equalsIgnoreCase(status.getEventStatusName())) {
                                            return Mono.error(new EventNotActiveException(ErrorState.EVENT_NOT_ACTIVE));
                                        }

                                        // Если все проверки пройдены - создаем запись
                                        EventUserListEntity newJoin = new EventUserListEntity();
                                        newJoin.setEventId(eventId);
                                        newJoin.setUserId(userId);
                                        newJoin.setRoleId(4); // роль участника

                                        return eventUserListRepository.save(newJoin)
                                                .thenReturn("Заявка успешно отправлена");
                                    })
                            );
                });
    }
}