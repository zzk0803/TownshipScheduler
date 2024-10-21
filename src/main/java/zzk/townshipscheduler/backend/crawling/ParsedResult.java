package zzk.townshipscheduler.backend.crawling;

import java.util.LinkedHashMap;
import java.util.Set;

public record ParsedResult(LinkedHashMap<String, ParsedResultSegment> map) {

    public Set<String> segmentNameSet() {
        return map.keySet();
    }

    public ParsedResultSegment getSegment(String category) {
        return this.map.get(category);
    }

    public LinkedHashMap<String, ParsedResultSegment> groupByTable() {
        return map;
    }

}
