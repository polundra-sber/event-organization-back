package ru.eventorg.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("event_user_list")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventUserListEntity {
    @Id
    private Integer eventUserListId;
    private Integer eventId;
    private String userId;
    private Integer roleId;

}
