package zzk.townshipscheduler.backend.crawling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;
import zzk.townshipscheduler.backend.persistence.dao.*;
import zzk.townshipscheduler.backend.service.ProductHierarchyAndGraphComponent;

import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
 class TownshipDataPersistProcessor {

    private final ProductEntityRepository productEntityRepository;

    private final ProductManufactureInfoEntityRepository productManufactureInfoEntityRepository;

    private final ProductMaterialsRelationRepository productMaterialsRelationRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final WikiCrawledParsedCoordCellEntityRepository wikiCrawledParsedCoordCellEntityRepository;

    private final ProductHierarchyAndGraphComponent productHierarchyAndGraphComponent;

    private final TransactionTemplate transactionTemplate;

    public void process(CrawledResult crawledResult) {
        log.info(" persist TownshipCoordCell and TownshipCrawled");
        TreeMap<CrawledDataCoordinate, CrawledDataCell> crawledResultMap = crawledResult.crawledDataCellTreeMap();
        crawledResultMap.forEach((coordinate, cell) -> {
            wikiCrawledParsedCoordCellEntityRepository.save(new WikiCrawledParsedCoordCellEntity(coordinate, cell));
        });
    }

    public PersistResult process(TransferResult transferResult) {
        List<ProductEntity> savedProductEntities = transactionTemplate.execute(_ -> {
            log.info("going to persist goods");
            List<ProductEntity> productEntityArrayList = transferResult.productEntityArrayList();
            List<ProductEntity> savedProductList = productEntityRepository.saveAll(productEntityArrayList);
            log.info("persist goods......done");
            return savedProductList;
        });
        return new PersistResult(savedProductEntities);
    }

}
