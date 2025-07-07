package ru.eventorg.controller;

import lombok.AllArgsConstructor;
import org.openapitools.api.TaskListApi;
import org.openapitools.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.TaskListItemCustom;
import ru.eventorg.dto.TaskListResponse;
import ru.eventorg.service.TaskListService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class TaskListController implements TaskListApi {

    TaskListService taskListService;

    @Override
    public Mono<ResponseEntity<TaskListItem>> addTaskToTasksList(Integer eventId, Mono<TaskListItemCreator> taskListItemCreator, ServerWebExchange exchange) throws Exception {
        return taskListItemCreator
                .flatMap(creator -> taskListService.addTask(eventId, creator))
                .map(this::toTaskListItemModel)
                .map(task -> ResponseEntity.status(HttpStatus.CREATED).body(task));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteTaskFromTasksList(Integer eventId, Integer taskId, ServerWebExchange exchange) {
        return taskListService.deleteTask(eventId, taskId)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Override
    public Mono<ResponseEntity<TaskListItem>> editTaskInTasksList(Integer eventId, Integer taskId, Mono<TaskListItemEditor> taskListItemEditor, ServerWebExchange exchange) throws Exception {
        return taskListItemEditor
                .flatMap(editor -> taskListService.editTask(eventId, taskId, editor))
                .map(this::toTaskListItemModel)
                .map(task -> ResponseEntity.ok().body(task));
    }

    @Override
    public Mono<ResponseEntity<GetTasksList200Response>> getTasksList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return taskListService.getTasks(eventId)
                .map(this::toGetTasksList200ResponseModel)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TaskListItem>> takeTaskFromTasksList(Integer eventId, Integer taskId, ServerWebExchange exchange) throws Exception {
        return taskListService.takeTask(eventId, taskId)
                .thenReturn(ResponseEntity.ok().build());
    }

    public TaskListItem toTaskListItemModel(TaskListItemCustom entity) {
        TaskListItem apiTaskListItem = new TaskListItem();
        apiTaskListItem.setTaskId(entity.getTaskId());
        apiTaskListItem.setTaskName(entity.getTaskName());
        apiTaskListItem.setTaskDescription(entity.getTaskDescription());
        apiTaskListItem.setTaskStatusName(entity.getStatusName());
        apiTaskListItem.setResponsibleLogin(entity.getResponsibleUserLogin());
        apiTaskListItem.setResponsibleName(entity.getResponsibleUserName());
        apiTaskListItem.setResponsibleSurname(entity.getResponsibleUserSurname());
        apiTaskListItem.setDeadlineDate(entity.getDeadlineDate());
        apiTaskListItem.deadlineTime(entity.getDeadlineTime() != null ? entity.getDeadlineTime().toString() : null);
        return apiTaskListItem;
    }

    public GetTasksList200Response toGetTasksList200ResponseModel(TaskListResponse response) {
        GetTasksList200Response apiGetTasksList200Response = new GetTasksList200Response();
        apiGetTasksList200Response.setEventDate(response.getDeadlineDate());
        apiGetTasksList200Response.setEventTime(response.getDeadlineTime() != null ?
                response.getDeadlineTime().toString() : null);

        // Преобразуем каждый элемент списка
        List<TaskListItem> taskListItems = response.getTasks().stream()
                .map(this::toTaskListItemModel)
                .collect(Collectors.toList());

        apiGetTasksList200Response.setTasks(taskListItems);
        return apiGetTasksList200Response;
    }

}
