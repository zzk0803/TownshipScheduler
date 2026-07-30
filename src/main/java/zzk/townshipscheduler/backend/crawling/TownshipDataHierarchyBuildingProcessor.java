package zzk.townshipscheduler.backend.crawling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;
import zzk.townshipscheduler.backend.persistence.dao.*;
import zzk.townshipscheduler.backend.service.ProductHierarchyAndGraphComponent;

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
        List<ProductEntity> savedProductEntities = transferResult.productEntityArrayList();
        List<ProductManufactureInfoEntity> productManufactureInfoEntities = new ArrayList<>();
        List<FieldFactoryInfoEntity> fieldFactoryInfoEntities = new ArrayList<>();
        List<ProductMaterialsRelation> productMaterialsRelations = new ArrayList<>();
        log.info("bonus...calc product manufacture info");
        log.info("bonus...map to factory info and persist");
        productHierarchyAndGraphComponent.calcProductsHierarchies();
        return transactionTemplate.execute(_ -> {
                    Objects.requireNonNull(savedProductEntities)
                            .stream()
                            .collect(Collectors.groupingBy(ProductEntity::getCategory))
                            .forEach((category, productEntities) -> {
                                transactionTemplate.executeWithoutResult(_ -> {
                                    FieldFactoryInfoEntity newFieldFactoryInfo = new FieldFactoryInfoEntity();
                                    newFieldFactoryInfo.setCategory(category);
                                    newFieldFactoryInfo.setLevel(
                                            productEntities.stream()
                                                    .map(ProductEntity::getLevel)
                                                    .min(Integer::compareTo)
                                                    .orElseThrow()
                                    );

                                    productEntities.forEach(
                                            productEntity -> {
                                                Set<ProductManufactureInfoEntity> calcedManufactureInfoSet
                                                        = productHierarchyAndGraphComponent.calcManufactureInfoSet(productEntity);
                                                if (!productEntity.attacheProductManufactureInfoCollection(calcedManufactureInfoSet)) {
                                                    log.warn("{} attacheProductManufactureInfoCollection not all success", productEntity.getName());
                                                }
                                                if (!newFieldFactoryInfo.attacheProductManufactureInfoCollection(calcedManufactureInfoSet)) {
                                                    log.warn("{} attacheProductManufactureInfoCollection not all success", newFieldFactoryInfo.getCategory());
                                                }

                                                productEntity.attacheProductManufactureInfoCollection(calcedManufactureInfoSet);
                                                productMaterialsRelations.addAll(
                                                        calcedManufactureInfoSet.stream()
                                                                .flatMap(manufactureInfoEntity -> manufactureInfoEntity.getProductMaterialsRelations().stream())
                                                                .toList()
                                                );
                                                productManufactureInfoEntities.addAll(calcedManufactureInfoSet);
                                            }
                                    );
                                    fieldFactoryInfoEntityRepository.save(newFieldFactoryInfo);
                                    productEntityRepository.saveAllAndFlush(productEntities);
                                    fieldFactoryInfoEntities.add(newFieldFactoryInfo);
                                });
                            });

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
