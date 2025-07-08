package ru.eventorg.dto;

import lombok.Data;
import ru.eventorg.entity.StuffEntity;

@Data
public class StuffWithEventDto {
    private StuffEntity stuff;
    private Integer eventId;
    private String eventName;


    public StuffWithEventDto(StuffEntity stuff, Integer eventId, String eventName) {
        this.stuff = stuff;
        this.eventId = eventId;
        this.eventName = eventName;
    }

    public StuffWithEventDto() {
        this.stuff = new StuffEntity();
        this.eventId = null;
        this.eventName = null;
    }
}
