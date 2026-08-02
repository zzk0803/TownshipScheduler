package zzk.townshipscheduler.backend.crawling;

import java.util.TreeMap;

public record CrawledResult(
        TreeMap<CrawledDataCoordinate, CrawledDataCell> crawledDataCellTreeMap
) {

}
