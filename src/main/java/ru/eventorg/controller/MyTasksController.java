package ru.eventorg.controller;

import lombok.AllArgsConstructor;
import org.openapitools.api.MyTasksApi;
import org.openapitools.model.MyTaskListItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.MyTaskListItemCustom;
import ru.eventorg.service.MyTasksService;

@RestController
@AllArgsConstructor
public class MyTasksController implements MyTasksApi {
    private final MyTasksService taskService;


    @Override
    public Mono<ResponseEntity<Void>> denyTaskInMyTasksList(Integer taskId, ServerWebExchange exchange) {
        return taskService.denyTask(taskId)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Override
    public Mono<ResponseEntity<Flux<MyTaskListItem>>> getMyTasksList(ServerWebExchange exchange) {
        Flux<MyTaskListItem> tasks = taskService.getMyTasksListCustom()
                .map(this::convertToMyTaskListItem);
        return Mono.just(ResponseEntity.ok(tasks));
    }

    @Override
    public Mono<ResponseEntity<Void>> markTaskCompletedInMyTasksList(Integer taskId, ServerWebExchange exchange) {
        return taskService.completeTask(taskId)
                .thenReturn(ResponseEntity.ok().build());
    }

    private MyTaskListItem convertToMyTaskListItem(MyTaskListItemCustom custom) {
        return new MyTaskListItem()
                .eventId(custom.getEventId())
                .eventName(custom.getEventName())
                .taskId(custom.getTaskId())
                .taskName(custom.getTaskName())
                .taskDescription(custom.getTaskDescription())
                .taskStatusName(custom.getStatusName())
                .deadlineDate(custom.getDeadlineDate())
                .deadlineTime(custom.getDeadlineTime() != null ? custom.getDeadlineTime().toString() : null);
    }
}
