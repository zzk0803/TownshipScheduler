package zzk.townshipscheduler.backend.crawling;

import io.arxila.javatuples.Trio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.ProductHierarchyAndGraphComponent;
import zzk.townshipscheduler.backend.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TownshipDataHierarchyBuildingProcessor {

    private final ProductEntityRepository productEntityRepository;

    private final ProductManufactureInfoEntityRepository productManufactureInfoEntityRepository;

    private final ProductMaterialsRelationRepository productMaterialsRelationRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final WikiCrawledParsedCoordCellEntityRepository wikiCrawledParsedCoordCellEntityRepository;

    private final ProductHierarchyAndGraphComponent productHierarchyAndGraphComponent;

    private final TransactionTemplate transactionTemplate;

    public HierarchyResult process(PersistResult transferResult) {
        List<ProductManufactureInfoEntity> productManufactureInfoEntities = new ArrayList<>();
        List<FieldFactoryInfoEntity> fieldFactoryInfoEntities = new ArrayList<>();
        List<ProductMaterialsRelation> productMaterialsRelations = new ArrayList<>();
        log.info("bonus...calc product manufacture info");
        log.info("bonus...map to factory info and persist");
        productHierarchyAndGraphComponent.calcProductsHierarchies();
        return transactionTemplate.execute(
                _ -> {
                    List<ProductEntity> savedProductEntities = productEntityRepository.findAllById(
                            transferResult.productEntityArrayList()
                                    .stream()
                                    .map(ProductEntity::getId)
                                    .collect(Collectors.toSet())
                    );

                    savedProductEntities.stream()
                            .map(
                                    productEntity -> {
                                        Set<ProductManufactureInfoEntity> calcedManufactureInfoSet
                                                = productHierarchyAndGraphComponent.calcManufactureInfoSet(productEntity);
                                        if (!productEntity.addProductManufactureInfos(calcedManufactureInfoSet)) {
                                            log.warn("{} attacheProductManufactureInfoCollection not all success", productEntity.getName());
                                        }
                                        return transactionTemplate.execute(_ -> productEntityRepository.saveAndFlush(productEntity));
                                    }
                            )
                            .peek(
                                    productEntity -> {
                                        Set<ProductManufactureInfoEntity> manufactureInfoEntities = productEntity.getManufactureInfoEntities();
                                        productMaterialsRelations.addAll(
                                                manufactureInfoEntities
                                                        .stream()
                                                        .flatMap(manufactureInfoEntity -> manufactureInfoEntity.getProductMaterialsRelations()
                                                                .stream())
                                                        .toList()
                                        );
                                        productManufactureInfoEntities.addAll(manufactureInfoEntities);
                                    }
                            )
                            .collect(
                                    Collectors.collectingAndThen(
                                            Collectors.groupingBy(ProductEntity::getCategory),
                                            (categoryProductEntitiesMap) -> {
                                                return categoryProductEntitiesMap.entrySet()
                                                        .stream()
                                                        .map(stringListEntry -> {
                                                            String category = stringListEntry.getKey();
                                                            var productEntities = stringListEntry.getValue();

                                                            FieldFactoryInfoEntity newFieldFactoryInfo = new FieldFactoryInfoEntity();
                                                            newFieldFactoryInfo.setCategory(category);
                                                            return new Trio<>(
                                                                    productEntities.stream()
                                                                            .map(ProductEntity::getLevel)
                                                                            .min(Integer::compareTo)
                                                                            .orElseThrow(), newFieldFactoryInfo, productEntities
                                                            );
                                                        })
                                                        .sorted(Comparator.comparingInt(Trio::value0))
                                                        .collect(Collectors.toCollection(LinkedHashSet::new));
                                            }
                                    )
                            )
                            .forEach(
                                    (trio) -> {
                                        transactionTemplate.execute(_ -> {
                                            Integer level = trio.value0();
                                            FieldFactoryInfoEntity fieldFactoryInfoEntity = trio.value1();
                                            List<ProductEntity> productEntities = (List<ProductEntity>) trio.value2();
                                            fieldFactoryInfoEntity.setLevel(level);

                                            FieldFactoryInfoEntity savedFieldFactoryInfoEntity = fieldFactoryInfoEntityRepository.saveAndFlush(fieldFactoryInfoEntity);
                                            savedFieldFactoryInfoEntity.addProductEntities(productEntities);

                                            FieldFactoryInfoEntity updatedFieldFactoryInfoEntity = fieldFactoryInfoEntityRepository.save(savedFieldFactoryInfoEntity);
                                            fieldFactoryInfoEntities.add(updatedFieldFactoryInfoEntity);
                                            return updatedFieldFactoryInfoEntity;
                                        });
                                    }
                            );

                    log.info("bonus...map to factory info and persist.....done");
                    log.info("bonus...calc product manufacture info......done");
                    log.info("savedProductEntities size:{}", savedProductEntities.size());
                    log.info("productManufactureInfoEntities size:{}", productManufactureInfoEntities.size());
                    log.info("fieldFactoryInfoEntities size:{}", fieldFactoryInfoEntities.size());
                    log.info("productMaterialsRelations size:{}", productMaterialsRelations.size());
                    return new HierarchyResult(savedProductEntities, productManufactureInfoEntities, fieldFactoryInfoEntities, productMaterialsRelations);
                }
        );
    }

}
