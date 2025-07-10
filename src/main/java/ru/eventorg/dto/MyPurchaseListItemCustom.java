package ru.eventorg.dto;

import lombok.Data;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;

@Data
public class MyPurchaseListItemCustom {
    private PurchaseEntity purchase;
    private UserProfileEntity responsibleUser;
    private Boolean hasReceipt;
    private String eventName;

    public MyPurchaseListItemCustom(PurchaseEntity purchase, UserProfileEntity responsibleUser, Boolean hasReceipt, String eventName) {
        this.purchase = purchase;
        this.responsibleUser = responsibleUser;
        this.hasReceipt = hasReceipt;
        this.eventName = eventName;
    }

    public MyPurchaseListItemCustom() {
        this.purchase = new PurchaseEntity();
        this.responsibleUser = new UserProfileEntity();
        this.hasReceipt = null;
        this.eventName = null;
    }
}
