package ru.eventorg.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.exception.EventNotExistException;
import ru.eventorg.repository.EventEntityRepository;
import ru.eventorg.repository.PurchaseEntityRepository;

import java.util.Optional;

@Service
public class PurchaseListService {
    private final PurchaseEntityRepository purchaseEntityRepository;

    public PurchaseListService(PurchaseEntityRepository purchaseEntityRepository) {
        this.purchaseEntityRepository = purchaseEntityRepository;
    }

    public Flux<PurchaseEntity> getPurchasesByEventId(Integer eventId) {
        return purchaseEntityRepository.getPurchaseEntitiesByEventId(eventId)
                .switchIfEmpty(Flux.error(new EventNotExistException()));
    }
}
