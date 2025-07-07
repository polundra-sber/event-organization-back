package ru.eventorg.service;

import io.r2dbc.spi.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.repository.ReceiptListEntityRepository;

@Service
@RequiredArgsConstructor
public class CostListService {
    private final ReceiptListEntityRepository receiptListEntityRepository;

    public Mono<Boolean> hasReceipt(Integer purchaseId) {
        return receiptListEntityRepository.existsReceiptListEntityByPurchaseId(purchaseId);
    }
}
