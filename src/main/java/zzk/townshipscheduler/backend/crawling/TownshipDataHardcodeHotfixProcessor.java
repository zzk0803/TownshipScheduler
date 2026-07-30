package zzk.townshipscheduler.backend.crawling;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.dao.FieldFactoryInfoEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.ProductManufactureInfoEntityRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
class TownshipDataHardcodeHotfixProcessor {

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final ProductManufactureInfoEntityRepository productManufactureInfoEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final TransactionTemplate transactionTemplate;

    public void process() {
        log.info("going to hardcode fix factoryinfo");
        Map<String, MendingData> instanceAmendMap = Map.of(
                "Cowshed",
                MendingData.builder()
                        .productNameList(List.of("Milk"))
                        .level(1)
                        .instanceAmount(3)
                        .build(),
                "Chicken Coop",
                MendingData.builder()
                        .productNameList(List.of("Egg"))
                        .level(5)
                        .instanceAmount(3)
                        .build(),
                "Sheep Farm",
                MendingData.builder()
                        .productNameList(List.of("Wool"))
                        .level(10)
                        .instanceAmount(2)
                        .build(),
                "Apiary",
                MendingData.builder()
                        .productNameList(List.of("Honeycombs"))
                        .level(35)
                        .instanceAmount(2)
                        .build(),
                "Pig Farm",
                MendingData.builder()
                        .productNameList(List.of("Bacon"))
                        .level(42)
                        .instanceAmount(2)
                        .build(),
                "Duck Feeder",
                MendingData.builder()
                        .productNameList(List.of("Down Feather", "Colorful Feather"))
                        .level(48)
                        .instanceAmount(1)
                        .build(),
                "Otter Pond",
                MendingData.builder()
                        .productNameList(List.of("Seaweed", "Scallop", "Pearls"))
                        .level(58)
                        .instanceAmount(1)
                        .build(),
                "Mushroom Farm",
                MendingData.builder()
                        .productNameList(List.of("Mushroom"))
                        .level(63)
                        .instanceAmount(1)
                        .build()
        );

        record ProductEntityTempRecord(
                String productName,
                ProductEntity productEntity,
                Collection<ProductManufactureInfoEntity> productManufactureInfoEntities
        ) {

        }

        AtomicReference<Set<ProductEntityTempRecord>> tempTableReference = new AtomicReference<>();
        Optional<FieldFactoryInfoEntity> farmBuildings
                = fieldFactoryInfoEntityRepository.findByCategory("Farm Buildings");
        farmBuildings.ifPresent(
                fieldFactoryInfoEntity -> {
                    Set<ProductEntity> relatedProducts = fieldFactoryInfoEntity.getProductEntities();
                    Set<ProductEntityTempRecord> ProductEntityTempRecords = relatedProducts.stream()
                            .map(productEntity -> new ProductEntityTempRecord(
                                    productEntity.getName(),
                                    productEntity,
                                    productEntity.getManufactureInfoEntities()
                                            .stream()
                                            .toList()
                            ))
                            .collect(Collectors.toSet());
                    tempTableReference.set(ProductEntityTempRecords);

                    fieldFactoryInfoEntity.clearProductEntity();
                    transactionTemplate.executeWithoutResult(_ -> fieldFactoryInfoEntityRepository.save(fieldFactoryInfoEntity));
                    transactionTemplate.executeWithoutResult(_ -> fieldFactoryInfoEntityRepository.delete(fieldFactoryInfoEntity));
                }
        );

        List<String> list = tempTableReference.get()
                .stream()
                .map(ProductEntityTempRecord::productName)
                .distinct()
                .sorted()
                .toList();
        List<String> list1 = instanceAmendMap.values()
                .stream()
                .map(MendingData::productNameList)
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList();
        if (!list.equals(list1)) {
            log.info("tempTableReference :: {}", list);
            log.info("instanceAmendMap :: {}", list1);
            throw new IllegalStateException("tempTableReference products not equal instanceAmendMap products");
        }

        instanceAmendMap.forEach(
                (factoryName, mendingData) -> {
                    FieldFactoryInfoEntity fieldFactoryInfo = createFieldFactoryInfo(factoryName, mendingData,name->productEntityRepository.queryByName(name).orElseThrow());
                    FieldFactoryInfoEntity savedFieldFactory = transactionTemplate.execute(status -> fieldFactoryInfoEntityRepository.saveAndFlush(fieldFactoryInfo));
                }
        );

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<FieldFactoryInfoEntity> feedMillOptional
                    = fieldFactoryInfoEntityRepository.findByCategory("Feed Mill");
            FieldFactoryInfoEntity feedMill = feedMillOptional.orElseThrow();
            feedMill.setLevel(3);
            feedMill.setDefaultInstanceAmount(1);
            feedMill.setDefaultProducingCapacity(3);
            feedMill.setDefaultReapWindowCapacity(6);
            feedMill.setMaxInstanceAmount(3);
            feedMill.setMaxProducingCapacity(7);
            feedMill.setMaxReapWindowCapacity(8);
            fieldFactoryInfoEntityRepository.save(feedMill);
        });

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<FieldFactoryInfoEntity> fieldOptional
                    = fieldFactoryInfoEntityRepository.findByCategory("Crops");
            FieldFactoryInfoEntity field = fieldOptional.orElseThrow();
            field.setCategory(FieldFactoryInfoEntity.FIELD_CATEGORY_CRITERIA);
            field.setLevel(1);
            field.setProducingType(ProducingStructureType.SLOT);
            field.setBoolCategoryField(true);
            field.setDefaultInstanceAmount(6);
            field.setDefaultProducingCapacity(1);
            field.setDefaultReapWindowCapacity(1);
            field.setMaxInstanceAmount(146);
            field.setMaxProducingCapacity(1);
            field.setMaxReapWindowCapacity(1);
            fieldFactoryInfoEntityRepository.save(field);
        });

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<FieldFactoryInfoEntity> islandShipOptional
                    = fieldFactoryInfoEntityRepository.findByCategory("Islands and Ships");
            FieldFactoryInfoEntity islandShip = islandShipOptional.orElseThrow();
            islandShip.setCategory("IslandsShip");
            islandShip.setLevel(29);
            islandShip.setProducingType(ProducingStructureType.SLOT);
            islandShip.setDefaultInstanceAmount(1);
            islandShip.setDefaultProducingCapacity(1);
            islandShip.setDefaultReapWindowCapacity(3);
            islandShip.setMaxInstanceAmount(4);
            islandShip.setMaxProducingCapacity(1);
            islandShip.setMaxReapWindowCapacity(3);
            fieldFactoryInfoEntityRepository.save(islandShip);
        });

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<FieldFactoryInfoEntity> foundryOptional = fieldFactoryInfoEntityRepository.findByCategory("Foundry");
            FieldFactoryInfoEntity foundry = foundryOptional.orElseThrow();
            foundry.setLevel(21);
            foundry.setProducingType(ProducingStructureType.SLOT);
            foundry.setDefaultInstanceAmount(1);
            foundry.setDefaultProducingCapacity(1);
            foundry.setDefaultReapWindowCapacity(1);
            foundry.setMaxInstanceAmount(3);
            foundry.setMaxProducingCapacity(1);
            foundry.setMaxReapWindowCapacity(1);
            fieldFactoryInfoEntityRepository.save(foundry);
        });

        log.info("going to hardcode fix factoryinfo method over");
    }

    private @NonNull FieldFactoryInfoEntity createFieldFactoryInfo(String factoryName, MendingData mendingData, Function<String,ProductEntity> nameToProductFunction) {
        FieldFactoryInfoEntity fieldFactoryInfo = new FieldFactoryInfoEntity();
        fieldFactoryInfo.setCategory(factoryName);
        fieldFactoryInfo.setLevel(mendingData.level());
        fieldFactoryInfo.setProducingType(ProducingStructureType.SLOT);
        fieldFactoryInfo.setDefaultInstanceAmount(1);
        fieldFactoryInfo.setDefaultProducingCapacity(3);
        fieldFactoryInfo.setDefaultReapWindowCapacity(3);
        fieldFactoryInfo.setMaxInstanceAmount(mendingData.instanceAmount());
        fieldFactoryInfo.setMaxProducingCapacity(6);
        fieldFactoryInfo.setMaxReapWindowCapacity(6);
        mendingData.productNameList().stream()
                .map(nameToProductFunction)
                .forEach(fieldFactoryInfo::addProductEntity);
        return fieldFactoryInfo;
    }

    @Builder
    public record MendingData(
            List<String> productNameList,
            int level,
            int instanceAmount
    ) {

    }

}
