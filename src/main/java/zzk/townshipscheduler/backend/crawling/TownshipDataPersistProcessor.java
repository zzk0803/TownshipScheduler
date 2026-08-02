package zzk.townshipscheduler.backend.crawling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.ProductHierarchyAndGraphComponent;
import zzk.townshipscheduler.backend.persistence.*;

import java.util.List;
import java.util.TreeMap;

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
        List<ProductEntity> productEntityArrayList = transferResult.productEntityArrayList();
        return new PersistResult(
                transactionTemplate.execute(_ -> {
                    log.info("going to persist goods");
                    List<ProductEntity> savedProductList = productEntityRepository.saveAllAndFlush(productEntityArrayList);
                    log.info("persist goods......done");
                    return savedProductList;
                })
        );
    }

}
