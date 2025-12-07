package zzk.townshipscheduler.backend.crawling;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;
import zzk.townshipscheduler.backend.dao.*;
import zzk.townshipscheduler.backend.service.ProductService;

import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class TownshipDataPersistProcessor {

    public static final Logger logger = LoggerFactory.getLogger(TownshipDataPersistProcessor.class);

    private final ProductEntityRepository productEntityRepository;

    private final ProductManufactureInfoEntityRepository productManufactureInfoEntityRepository;

    private final ProductMaterialsRelationRepository productMaterialsRelationRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final WikiCrawledParsedCoordCellEntityRepository wikiCrawledParsedCoordCellEntityRepository;

    private final ProductService productService;

    private final TransactionTemplate transactionTemplate;

    public void process(CrawledResult crawledResult) {
        logger.info(" persist TownshipCoordCell and TownshipCrawled");
        TreeMap<CrawledDataCoordinate, CrawledDataCell> crawledResultMap = crawledResult.crawledDataCellTreeMap();
        crawledResultMap.forEach((coordinate, cell) -> {
            wikiCrawledParsedCoordCellEntityRepository.save(new WikiCrawledParsedCoordCellEntity(coordinate, cell));
        });
    }

    public void process(TransferResult transferResult) {
        List<ProductEntity> savedProductEntities = transactionTemplate.execute(_ -> {
            logger.info("going to persist goods");
            List<ProductEntity> productEntityArrayList = transferResult.productEntityArrayList();
            List<ProductEntity> savedProductList = productEntityRepository.saveAll(productEntityArrayList);
            logger.info("persist goods......done");

            logger.info("bonus...map to factory info and persist");
            Objects.requireNonNull(savedProductList)
                    .stream()
                    .collect(Collectors.groupingBy(ProductEntity::getCategory))
                    .forEach((category, productEntities) -> {
                        FieldFactoryInfoEntity fieldFactoryInfo = new FieldFactoryInfoEntity();
                        fieldFactoryInfo.setCategory(category);
                        fieldFactoryInfo.setLevel(
                                productEntities.stream()
                                        .map(ProductEntity::getLevel)
                                        .min(Integer::compareTo)
                                        .orElseThrow()
                        );
                        transactionTemplate.executeWithoutResult(
                                _ -> {
                                    FieldFactoryInfoEntity savedFieldFactoryInfo
                                            = fieldFactoryInfoEntityRepository.save(fieldFactoryInfo);
                                    savedFieldFactoryInfo.attacheProductEntities(productEntities);
                                }
                        );
                    });
            logger.info("bonus...map to factory info and persist.....done");

            logger.info("bonus...calc product manufacture info");
            productService.calcGoodsHierarchies();//warm data
            savedProductList.forEach(productEntity -> {
                transactionTemplate.executeWithoutResult(_ -> {
                    List<ProductManufactureInfoEntity> manufactureInfoEntities
                            = productManufactureInfoEntityRepository.saveAll(productService.calcManufactureInfoSet(productEntity)
                    );
                    for (ProductManufactureInfoEntity manufactureInfoEntity : manufactureInfoEntities) {
                        productEntity.attacheProductManufactureInfo(manufactureInfoEntity);
                    }
                });
            });
            logger.info("bonus...calc product manufacture info......done");
            return savedProductList;
        });
    }

}
