package zzk.townshipscheduler.backend.crawling;

import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public record CrawledResult(
        TreeMap<CrawledDataCoordinate, CrawledDataCell> crawledDataCellTreeMap
) {

}
