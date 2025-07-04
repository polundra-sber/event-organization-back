package ru.eventorg.dto;

import lombok.Data;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;

@Data
public class PurchaseWithUserDto {
    private PurchaseEntity purchase;
    private UserProfileEntity responsibleUser;

    public PurchaseWithUserDto() {
        this.purchase = new PurchaseEntity(); // Пустая покупка
        this.responsibleUser = null; // Нет ответственного
    }

    public PurchaseWithUserDto(PurchaseEntity purchase, UserProfileEntity responsibleUser) {
        this.purchase = purchase;
        this.responsibleUser = responsibleUser;
    }
}