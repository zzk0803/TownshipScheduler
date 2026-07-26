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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        List<FieldFactoryInfoEntity> fieldFactoryInfoEntities = new ArrayList<>();
        List<ProductMaterialsRelation> productMaterialsRelations = new ArrayList<>();
        return transactionTemplate.execute(_ -> {
            log.info("bonus...map to factory info and persist");
            Objects.requireNonNull(savedProductEntities)
                    .stream()
                    .collect(Collectors.groupingBy(ProductEntity::getCategory))
                    .forEach((category, productEntities) -> {
                        FieldFactoryInfoEntity savedFieldFactoryInfo = transactionTemplate.execute(
                                _ -> {
                                    FieldFactoryInfoEntity newFieldFactoryInfo = new FieldFactoryInfoEntity();
                                    newFieldFactoryInfo.setCategory(category);
                                    newFieldFactoryInfo.setLevel(
                                            productEntities.stream()
                                                    .map(ProductEntity::getLevel)
                                                    .min(Integer::compareTo)
                                                    .orElseThrow()
                                    );
                                    FieldFactoryInfoEntity fieldFactoryInfoEntity
                                            = fieldFactoryInfoEntityRepository.save(newFieldFactoryInfo);
                                    fieldFactoryInfoEntity.attacheProductEntities(
                                            productEntityRepository.findAllById(
                                                    productEntities.stream().map(ProductEntity::getId).toList()
                                            )
                                    );
                                    return fieldFactoryInfoEntity;
                                }
                        );
                        fieldFactoryInfoEntities.add(savedFieldFactoryInfo);
                    });
            log.info("bonus...map to factory info and persist.....done");

            log.info("bonus...calc product manufacture info");
            productHierarchyAndGraphComponent.calcProductsHierarchies();//warm data
            savedProductEntities.forEach(
                    productEntity -> {
                        transactionTemplate.executeWithoutResult(_ -> {
                            Set<ProductManufactureInfoEntity> productManufactureInfoEntities
                                    = productHierarchyAndGraphComponent.calcManufactureInfoSet(
                                    productEntityRepository.findById(productEntity.getId()).orElseThrow()
                            );
                            List<ProductManufactureInfoEntity> manufactureInfoEntities
                                    = productManufactureInfoEntityRepository.saveAll(productManufactureInfoEntities);
                            List<ProductMaterialsRelation> materialsRelationList = manufactureInfoEntities.stream()
                                    .peek(productEntity::attacheProductManufactureInfo)
                                    .flatMap(manufactureInfoEntity -> manufactureInfoEntity.getProductMaterialsRelations().stream())
                                    .toList();
                            productMaterialsRelations.addAll(materialsRelationList);
                        });
                    }
            );
            log.info("bonus...calc product manufacture info......done");
            return new HierarchyResult(savedProductEntities, fieldFactoryInfoEntities, productMaterialsRelations);
        });
    }

}
