package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.eventorg.entity.ReceiptEntity;
import ru.eventorg.entity.ReceiptListEntity;
import ru.eventorg.repository.EventEntityRepository;
import ru.eventorg.repository.PurchaseEntityRepository;
import ru.eventorg.repository.ReceiptEntityRepository;
import ru.eventorg.repository.ReceiptListEntityRepository;
import ru.eventorg.service.enums.EventStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptService {
    private final EventEntityRepository eventRepo;
    private final PurchaseEntityRepository purchaseRepo;
    private final ReceiptListEntityRepository receiptListRepo;
    private final ReceiptEntityRepository receiptRepo;
    private final R2dbcEntityTemplate template;
    private final ResourceLoader resourceLoader;
    private final ReceiptEntityRepository receiptEntityRepository;
    private final ReceiptListEntityRepository receiptListEntityRepository;

    private final Integer DURATION_IN_DAYS = 30;

    @Value("${app.receipts.dir:/var/app/receipts}")
    private String receiptsDir;

    // 0:00 ежедневно
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public Mono<Void> cleanupOldReceipts() {
        LocalDate cutoff = LocalDate.now().minusDays(DURATION_IN_DAYS);
        List<Integer> statuses = List.of(EventStatus.COMPLETED.ordinal() + 1, EventStatus.DELETED.ordinal() + 1);

        return eventRepo
                .findByStatusIdInAndEventDateBefore(statuses, cutoff)
                .flatMap(event ->
                        purchaseRepo
                                .findAllByEventId(event.getEventId())
                                .flatMap(purchase ->
                                        receiptListRepo
                                                .findAllByPurchaseId(purchase.getPurchaseId())
                                                .flatMap(receiptList -> {
                                                    Integer rid = receiptList.getReceiptId();
                                                    return receiptRepo
                                                            .findById(rid)
                                                            .flatMap(receipt -> {
                                                                // Удаляем файл из файловой системы
                                                                Path file = Paths.get(receipt.getFilePath());
                                                                return Mono.fromRunnable(() -> {
                                                                            try {
                                                                                Files.deleteIfExists(file);
                                                                            } catch (Exception ignored) { }
                                                                        })
                                                                        .subscribeOn(Schedulers.boundedElastic())
                                                                        // Удаляем записи в БД
                                                                        .then(receiptRepo.deleteById(rid))
                                                                        .then(receiptListRepo.delete(receiptList));
                                                            });
                                                })
                                )
                )
                .then();
    }

    public Flux<Resource> getReceiptResources(Integer eventId, Integer purchaseId) {
        return getImagePathsByPurchaseId(purchaseId)
                .map(path -> resourceLoader.getResource("file:" + path));
    }

    public Flux<String> getImagePathsByPurchaseId(Integer purchaseId) {
        String sqlGetImagePath = """
                SELECT
                    r.file_path AS file_path
                FROM receipt_list rl
                JOIN receipt r ON r.receipt_id = rl.receipt_id
                WHERE rl.purchase_id = $1
                """;

        return template.getDatabaseClient()
                .sql(sqlGetImagePath)
                .bind(0, purchaseId)
                .map((row, meta) -> row.get("file_path", String.class))
                .all();
    }

    public Mono<Void> storeReceipts(Integer purchaseId, Flux<FilePart> fileParts) {
        return fileParts
                .flatMap(part -> {
                    String original = part.filename();
                    String ext = "";
                    int dot = original.lastIndexOf('.');
                    if (dot > 0) {
                        ext = original.substring(dot);
                    }

                    String filename = UUID.randomUUID().toString() + ext;
                    Path targetDir = Paths.get(receiptsDir, String.valueOf(purchaseId));
                    Path targetFile = targetDir.resolve(filename);

                    // Переносим создание директорий и сохранение файла в boundedElastic
                    return Mono.fromCallable(() -> {
                                if (!Files.exists(targetDir)) {
                                    Files.createDirectories(targetDir); // блокирующий вызов
                                }
                                return targetFile;
                            })
                            .subscribeOn(Schedulers.boundedElastic()) // выполняем в отдельном потоке
                            .flatMap(path ->
                                    part.transferTo(path)
                                            .then(receiptEntityRepository.save(new ReceiptEntity(null, path.toString())))
                                            .flatMap(savedReceipt ->
                                                    receiptListEntityRepository.save(
                                                            new ReceiptListEntity(null, purchaseId, savedReceipt.getReceiptId())
                                                    )
                                            )
                            );
                })
                .then();
    }
}
