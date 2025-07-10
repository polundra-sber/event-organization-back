package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.PurchaseNotExistException;
import ru.eventorg.exception.PurchaseNullResponsibleException;
import ru.eventorg.repository.PurchaseEntityRepository;

@Service
@RequiredArgsConstructor
public class PurchaseValidationService {
    private final PurchaseEntityRepository purchaseEntityRepository;

    public Mono<Void> purchaseExists(Integer purchaseId) {
        return purchaseEntityRepository.existsById(purchaseId)
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)));
    }

    public Mono<Void> purchaseInEvent(Integer purchaseId, Integer eventId) {
        return purchaseEntityRepository.existsPurchaseEntityByPurchaseIdAndEventId(purchaseId, eventId)
                .flatMap(exists -> exists
                ? Mono.empty()
                        : Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)));
    }

    public Mono<Void> allPurchasesHasResponsible(Integer eventId) {
        return purchaseEntityRepository.existsPurchaseEntitiesByEventIdAndResponsibleUserIsNull(eventId)
                .flatMap(exists -> exists
                ? Mono.error(new PurchaseNullResponsibleException(ErrorState.PURCHASE_DOES_NOT_HAVE_RESPONSIBLE))
                : Mono.empty());
    }
}
