package ru.eventorg.service;



import org.openapitools.model.StuffListItemCreator;
import org.openapitools.model.StuffListItemEditor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.StuffWithUserDto;
import ru.eventorg.entity.StuffEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.exception.AlreadyHasResponsibleException;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.StuffNotExistException;
import ru.eventorg.repository.StuffEntityRepository;
import ru.eventorg.repository.UserProfilesEntityRepository;


import static ru.eventorg.security.SecurityUtils.getCurrentUserLogin;

@Service
public class StuffListService {
    private final R2dbcEntityTemplate template;
    private final EventService eventService;
    private final RoleService roleService;
    private final UserProfilesEntityRepository userProfilesEntityRepository;
    private final StuffEntityRepository stuffEntityRepository;
    private final DatabaseClient databaseClient;
    private static final String GET_STUFFS_SQL = """
        SELECT
            s.stuff_id, s.stuff_name, s.stuff_description,
            s.responsible_user, s.event_id,
            up.login,
            up.name AS user_name,
            up.surname AS user_surname,
            up.comment_money_transfer AS user_comment
        FROM stuff s
        LEFT JOIN user_profile up ON s.responsible_user = up.login
        WHERE s.event_id = $1
        """;

    public StuffListService(R2dbcEntityTemplate template, EventService eventService, RoleService roleService, UserProfilesEntityRepository userProfilesEntityRepository, StuffEntityRepository stuffEntityRepository, DatabaseClient databaseClient) {
        this.template = template;
        this.eventService = eventService;
        this.roleService = roleService;
        this.userProfilesEntityRepository = userProfilesEntityRepository;
        this.stuffEntityRepository = stuffEntityRepository;
        this.databaseClient = databaseClient;
    }

    public Mono<StuffWithUserDto> addStuffToStuffsList(Integer eventId, Mono<StuffListItemCreator> stuffListItemCreator){
        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(roleService.checkIfOrganizerOrHigher(eventId, currentUserLogin))
                                .then(stuffListItemCreator)
                                .flatMap(creator ->
                                        roleService.validateIsParticipant(eventId, creator.getResponsibleLogin())
                                                .then(createAndSaveStuff(eventId, creator))
                                )
                );
    }



    public Flux<StuffWithUserDto> getStuffsList(Integer eventId) {
        return getCurrentUserLogin()
                .flatMapMany(userLogin ->
                        eventService.validateExists(eventId)
                                .then(roleService.validateIsParticipant(eventId, userLogin))
                                .thenMany(
                                        databaseClient.sql(GET_STUFFS_SQL)
                                                .bind(0, eventId)
                                                .fetch()
                                                .all()
                                                .flatMap(this::mapRowToStuffWithUserDto)
                                )
                                .switchIfEmpty(Flux.empty())
                );
    }


    public Mono<Void> deleteStuffFromStuffsList(Integer eventId, Integer stuffId) {
        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(stuffEntityRepository.findByStuffIdAndEventId(stuffId, eventId)
                                        .switchIfEmpty(Mono.error(new StuffNotExistException(ErrorState.STUFF_NOT_EXIST)))
                                        .then(roleService.checkIfOrganizerOrHigher(eventId, currentUserLogin))
                                        .then(stuffEntityRepository.deleteByStuffIdAndEventId(stuffId, eventId))
                                )
                );
    }


    public Mono<StuffWithUserDto> editStuffInStuffsList(
            Integer eventId,
            Integer stuffId,
            Mono<StuffListItemEditor> stuffListItemEditor) {

        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(stuffEntityRepository.findByStuffIdAndEventId(stuffId, eventId))
                                .switchIfEmpty(Mono.error(new StuffNotExistException(ErrorState.STUFF_NOT_EXIST)))
                                .then(roleService.checkIfOrganizerOrHigher(eventId, currentUserLogin))
                                .then(stuffListItemEditor.flatMap(editor ->
                                                updateAndSaveStuff(eventId, stuffId, editor)
                                        )
                                )
                );
    }

    public Mono<StuffWithUserDto> takeStuffFromStuffsList(Integer eventId, Integer stuffId) {
        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(roleService.validateIsParticipant(eventId, currentUserLogin))
                                .then(stuffEntityRepository.findByStuffIdAndEventId(stuffId, eventId))
                                .switchIfEmpty(Mono.error(new StuffNotExistException(ErrorState.STUFF_NOT_EXIST)))
                                .flatMap(stuff -> {
                                    if (stuff.getResponsibleUser() != null) {
                                        return Mono.error(new AlreadyHasResponsibleException(ErrorState.STUFF_ALREADY_HAS_RESPONSIBLE));
                                    }
                                    stuff.setResponsibleUser(currentUserLogin);
                                    return stuffEntityRepository.save(stuff)
                                            .flatMap(this::createStuffWithUserDto);
                                })
                );
    }



    //Вспомогательные методы
    private Mono<StuffWithUserDto> createAndSaveStuff(Integer eventId, StuffListItemCreator creator) {
        StuffEntity stuff = new StuffEntity();
        stuff.setStuffName(creator.getStuffName());
        stuff.setStuffDescription(creator.getStuffDescription());
        stuff.setResponsibleUser(creator.getResponsibleLogin());
        stuff.setEventId(eventId);
        return template.insert(stuff)
                .flatMap(this::createStuffWithUserDto);
    }


    private Mono<StuffWithUserDto> createStuffWithUserDto(StuffEntity stuff) {
        return userProfilesEntityRepository.findByLogin(stuff.getResponsibleUser())
                .map(user -> new StuffWithUserDto(stuff, user))
                .defaultIfEmpty(new StuffWithUserDto(stuff, null));
    }


    private Mono<StuffWithUserDto> mapRowToStuffWithUserDto(java.util.Map<String, Object> row) {
        StuffEntity stuff = new StuffEntity();
        stuff.setStuffId((Integer) row.get("stuff_id"));
        stuff.setStuffName((String) row.get("stuff_name"));
        stuff.setStuffDescription((String) row.get("stuff_description"));
        stuff.setResponsibleUser((String) row.get("responsible_user"));
        stuff.setEventId((Integer) row.get("event_id"));

        UserProfileEntity user = null;
        if (row.get("login") != null) {
            user = new UserProfileEntity();
            user.setLogin((String) row.get("login"));
            user.setName((String) row.get("user_name"));
            user.setSurname((String) row.get("user_surname"));
        }

        return Mono.just(new StuffWithUserDto(stuff, user));
    }

    private Mono<StuffWithUserDto> updateAndSaveStuff(
            Integer eventId,
            Integer stuffId,
            StuffListItemEditor editor) {

        return stuffEntityRepository.findByStuffIdAndEventId(stuffId, eventId)
                .flatMap(existing -> {
                    existing.setStuffName(editor.getStuffName());
                    existing.setStuffDescription(editor.getStuffDescription());
                    existing.setResponsibleUser(editor.getResponsibleLogin());

                    return roleService.validateIsParticipant(eventId, editor.getResponsibleLogin())
                            .then(stuffEntityRepository.save(existing))
                            .flatMap(this::createStuffWithUserDto);
                });
    }
}
