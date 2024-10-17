package zzk.project.vaadinproject.backend.crawling;

import java.util.TreeMap;

public record CrawledResult(TreeMap<RawDataCrawledCoord, RawDataCrawledCell> map) {

    public TreeMap<RawDataCrawledCoord, RawDataCrawledCell> get() {
        return map;
    }

}
