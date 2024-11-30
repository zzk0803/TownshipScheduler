package zzk.townshipscheduler.backend.crawling;

import java.util.TreeMap;

public record CrawledResult(TreeMap<CrawledDataCoordinate, CrawledDataCell> map) {

    public TreeMap<CrawledDataCoordinate, CrawledDataCell> get() {
        return map;
    }

}
