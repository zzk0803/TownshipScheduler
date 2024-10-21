package zzk.townshipscheduler.backend.crawling;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.backend.persistence.GoodsRepository;
import zzk.townshipscheduler.backend.persistence.TownshipCoordCell;
import zzk.townshipscheduler.backend.persistence.TownshipCoordCellRepository;

import java.util.List;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
class TownshipDataPersistProcessor {

    public static final Logger logger = LoggerFactory.getLogger(TownshipDataPersistProcessor.class);

    private final GoodsRepository goodsRepository;

    private final TownshipCoordCellRepository townshipCoordCellRepository;

    private final TransactionTemplate transactionTemplate;

    public void process(CrawledResult crawledResult) {
        logger.info(" persist TownshipCoordCell and TownshipCrawled");
        TreeMap<RawDataCrawledCoord, RawDataCrawledCell> crawledResultMap = crawledResult.map();
        crawledResultMap.forEach((coord, cell) -> {
            townshipCoordCellRepository.save(new TownshipCoordCell(coord, cell));
        });
    }

    public void process(TransferResult transferResult) {
        List<Goods> goodsArrayList = transferResult.goodsArrayList();

        logger.info("going to persist goods");
        List<Goods> savedResult = transactionTemplate.execute(ts -> goodsRepository.saveAllAndFlush(goodsArrayList));
        if (savedResult == null || savedResult.isEmpty()) {
            logger.warn("goods list persisted has some problem");
        } else {
            logger.info("goods list persisted done...{} saved", savedResult.size());
        }

    }

}
