package ru.eventorg.service;

import lombok.AllArgsConstructor;
import org.openapitools.model.TaskListItemCreator;
import org.openapitools.model.TaskListItemEditor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.TaskListItemCustom;
import ru.eventorg.dto.TaskListResponse;
import ru.eventorg.entity.TaskEntity;
import ru.eventorg.entity.TaskStatusEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.TaskNotFoundException;
import ru.eventorg.repository.EventEntityRepository;
import ru.eventorg.repository.TaskEntityRepository;
import ru.eventorg.repository.TaskStatusEntityRepository;
import ru.eventorg.repository.UserProfilesEntityRepository;
import ru.eventorg.security.SecurityUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TaskListService {
    private final TaskEntityRepository taskRepository;
    private final EventEntityRepository eventRepository;
    private final TaskStatusEntityRepository statusRepository;
    private final EventService eventService;
    private final RoleService roleService;
    private final UserProfilesEntityRepository userProfilesRepository;

    // Добавление задачи (возвращает TaskEntity)
    public Mono<TaskListItemCustom> addTask(Integer eventId, TaskListItemCreator taskCreator) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(roleService.checkIfOrganizerOrHigher(eventId, userLogin))
                                .then(Mono.defer(() -> {
                                    TaskEntity task = createTaskEntity(eventId, taskCreator);
                                    // Разрешаем сохранение с responsibleUser = null
                                    return taskRepository.save(task);
                                }))
                                .flatMap(savedTask -> {
                                    Mono<TaskStatusEntity> statusMono = statusRepository.findById(savedTask.getStatusId());

                                    Mono<UserProfileEntity> userMono = Mono.justOrEmpty(savedTask.getResponsibleUser())
                                            .flatMap(userProfilesRepository::findByLogin);

                                    return Mono.zip(statusMono, userMono)
                                            .map(tuple -> convertToTaskListItemCustom(
                                                    savedTask,
                                                    tuple.getT1(),
                                                    tuple.getT2()
                                            ));
                                })
                );}

    // Удаление задачи
    public Mono<Void> deleteTask(Integer eventId, Integer taskId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(roleService.checkIfOrganizerOrHigher(eventId, userLogin))
                                .then(taskRepository.getTaskEntityByEventIdAndTaskId(eventId, taskId))
                                .switchIfEmpty(Mono.error(new TaskNotFoundException(ErrorState.TASK_NOT_EXIST)))
                                .flatMap(taskRepository::delete)
                );
    }

    // Редактирование задачи (возвращает TaskEntity)
    public Mono<TaskListItemCustom> editTask(Integer eventId, Integer taskId, TaskListItemEditor editor) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(roleService.checkIfOrganizerOrHigher(eventId, userLogin))
                                .then(taskRepository.getTaskEntityByEventIdAndTaskId(eventId, taskId))
                                .switchIfEmpty(Mono.error(new TaskNotFoundException(ErrorState.TASK_NOT_EXIST)))
                                .flatMap(task -> {
                                    TaskEntity taskNew = updateTaskFromEditor(task, editor);
                                    // Разрешаем сохранение с responsibleUser = null
                                    return taskRepository.save(taskNew);
                                })
                                .flatMap(savedTask -> {
                                    Mono<TaskStatusEntity> statusMono = statusRepository.findById(savedTask.getStatusId());

                                    Mono<UserProfileEntity> userMono = Mono.justOrEmpty(savedTask.getResponsibleUser())
                                            .flatMap(userProfilesRepository::findByLogin);

                                    return Mono.zip(statusMono, userMono)
                                            .map(tuple -> convertToTaskListItemCustom(
                                                    savedTask,
                                                    tuple.getT1(),
                                                    tuple.getT2()
                                            ));
                                })
                );}

    public Mono<TaskListResponse> getTasks(Integer eventId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userLogin ->
                        eventService.validateExists(eventId)
                                .then(roleService.validateIsParticipant(eventId, userLogin))
                                .then(eventRepository.findById(eventId))
                                .flatMap(event -> {
                                    // Получаем все задачи для мероприятия
                                    Mono<List<TaskEntity>> tasksMono = taskRepository.getTaskEntitiesByEventId(eventId).collectList();

                                    // Получаем все статусы задач
                                    Mono<Map<Integer, TaskStatusEntity>> statusMapMono = tasksMono
                                            .flatMap(tasks -> {
                                                Set<Integer> statusIds = tasks.stream()
                                                        .map(TaskEntity::getStatusId)
                                                        .collect(Collectors.toSet());
                                                return statusRepository.findAllById(statusIds)
                                                        .collectMap(TaskStatusEntity::getTaskStatusId); // Используем правильный метод
                                            });
                                    // Получаем профили всех пользователей
                                    Mono<Map<String, UserProfileEntity>> userMapMono = tasksMono
                                            .flatMap(tasks -> {
                                                Set<String> logins = tasks.stream()
                                                        .map(TaskEntity::getResponsibleUser)
                                                        .filter(Objects::nonNull)
                                                        .collect(Collectors.toSet());
                                                return userProfilesRepository.findByLoginIn(logins)
                                                        .collectMap(UserProfileEntity::getLogin);
                                            });

                                    return Mono.zip(tasksMono, statusMapMono, userMapMono)
                                            .map(tuple -> {
                                                List<TaskEntity> tasks = tuple.getT1();
                                                Map<Integer, TaskStatusEntity> statusMap = tuple.getT2();
                                                Map<String, UserProfileEntity> userMap = tuple.getT3();

                                                List<TaskListItemCustom> taskItems = tasks.stream()
                                                        .map(task -> convertToTaskListItemCustom(
                                                                task,
                                                                statusMap.get(task.getStatusId()),
                                                                userMap.get(task.getResponsibleUser())
                                                        ))
                                                        .collect(Collectors.toList());

                                                return new TaskListResponse(
                                                        event.getEventDate(),
                                                        event.getEventTime(),
                                                        taskItems
                                                );
                                            });
                }));
    }

    public Mono<TaskEntity> takeTask(Integer eventId, Integer taskId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(roleService.validateIsParticipant(eventId, userLogin))
                                .then(taskRepository.getTaskEntityByEventIdAndTaskId(eventId, taskId))
                                .switchIfEmpty(Mono.error(new TaskNotFoundException(ErrorState.TASK_NOT_EXIST)))
                                .flatMap(task -> {
                                    task.setResponsibleUser(userLogin);
                                    return taskRepository.save(task);
                                }));
    }

    private TaskListItemCustom convertToTaskListItemCustom(
            TaskEntity task,
            TaskStatusEntity status,
            UserProfileEntity userProfile
    ) {
        return new TaskListItemCustom(
                task.getTaskId(),
                task.getTaskName(),
                task.getTaskDescription(),
                status != null ? status.getTaskStatusName() : null,
                task.getEventId(),
                userProfile != null ? userProfile.getLogin() : null,
                userProfile != null ? userProfile.getName() : null,
                userProfile != null ? userProfile.getSurname() : null,
                task.getDeadlineDate(),
                task.getDeadlineTime()
        );
    }

    private TaskEntity createTaskEntity(Integer eventId, TaskListItemCreator creator) {
        TaskEntity task = new TaskEntity();
        task.setTaskName(creator.getTaskName());
        task.setTaskDescription(creator.getTaskDescription());
        task.setStatusId(2);
        task.setEventId(eventId);
        task.setResponsibleUser(creator.getResponsibleLogin());
        task.setDeadlineDate(creator.getDeadlineDate());
        if (creator.getDeadlineTime() != null) {
            task.setDeadlineTime(LocalTime.parse(creator.getDeadlineTime()));
        } else {
            task.setDeadlineTime(null);
        }
        return task;
    }

    private TaskEntity updateTaskFromEditor(TaskEntity task, TaskListItemEditor editor) {
        task.setTaskName(editor.getTaskName());
        task.setTaskDescription(editor.getTaskDescription());
        task.setDeadlineDate(editor.getDeadlineDate());
        if (editor.getDeadlineTime() != null) {
            task.setDeadlineTime(LocalTime.parse(editor.getDeadlineTime()));
        } else {
            task.setDeadlineTime(null);
        }
        task.setResponsibleUser(editor.getResponsibleLogin());
        return task;
    }
}
