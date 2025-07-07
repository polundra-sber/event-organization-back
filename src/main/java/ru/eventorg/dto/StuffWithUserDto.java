package ru.eventorg.dto;

import lombok.Data;
import ru.eventorg.entity.StuffEntity;
import ru.eventorg.entity.UserProfileEntity;

@Data
public class StuffWithUserDto {
    private StuffEntity stuff;
    private UserProfileEntity responsibleUser;

    public StuffWithUserDto() {
        this.stuff = new StuffEntity();
        this.responsibleUser = null;
    }

    public StuffWithUserDto(StuffEntity stuff, UserProfileEntity responsibleUser) {
        this.stuff = stuff;
        this.responsibleUser = responsibleUser;
    }
}
