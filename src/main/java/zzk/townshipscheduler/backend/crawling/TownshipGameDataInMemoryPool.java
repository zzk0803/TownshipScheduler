package zzk.townshipscheduler.backend.crawling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class TownshipGameDataInMemoryPool {

    private static final Logger logger = LoggerFactory.getLogger(TownshipGameDataInMemoryPool.class);

    private final TreeMap<RawDataCrawledCoord, RawDataCrawledCell> rawCellMap = new TreeMap<>();

    private final TreeMap<RawDataCrawledCoord, RawDataCrawledCell> spanFixMap = new TreeMap<>();

    CrawledResult completeAndMend() {
        logger.info("append SENTINEL pair");

        //because parse algorithm take frame between two head row(which column size suppose to be one),last dataZone miss the row doesn't exist
        //so append a row so called "sentinel" make the algorithm make sense
        putForSave(
                new RawDataCrawledCoord("SENTINEL", Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE),
                new RawDataCrawledCell(
                        "SENTINEL",
                        "SENTINEL",
                        Collections.emptyList(),
                        Collections.emptyList(),
                        RawDataCrawledCell.Type.HEAD,
                        new RawDataCrawledCell.CellSpan(0, 0)
                )
        );

        MendProgress mendProgress = new MendProgress(rawCellMap, spanFixMap, new TreeMap<>());
        TreeMap<RawDataCrawledCoord, RawDataCrawledCell> processed = mendProgress.process();

        return new CrawledResult(processed);
    }

    void putForSave(RawDataCrawledCoord coord, RawDataCrawledCell cell) {
        rawCellMap.put(coord, cell);
    }

    void reset() {
        rawCellMap.clear();
        spanFixMap.clear();
    }

    void putForMend(RawDataCrawledCoord coord, RawDataCrawledCell cell) {
        spanFixMap.put(coord, cell);
    }

    private static class MendProgress {

        private final TreeMap<RawDataCrawledCoord, RawDataCrawledCell> rawCellMap;

        private final TreeMap<RawDataCrawledCoord, RawDataCrawledCell> spanFixMap;

        private final TreeMap<RawDataCrawledCoord, RawDataCrawledCell> resultMap;

        public MendProgress(
                TreeMap<RawDataCrawledCoord, RawDataCrawledCell> rawCellMap,
                TreeMap<RawDataCrawledCoord, RawDataCrawledCell> spanFixMap,
                TreeMap<RawDataCrawledCoord, RawDataCrawledCell> resultMap
        ) {
            this.rawCellMap = rawCellMap;
            this.spanFixMap = spanFixMap;
            this.resultMap = resultMap;
        }

        private TreeMap<RawDataCrawledCoord, RawDataCrawledCell> result() {
            return this.resultMap;
        }

        private TreeMap<RawDataCrawledCoord, RawDataCrawledCell> process() {
            logger.info("do mending...");
            //<table,<row,ProductsCrawledCell>>
            TreeMap<Integer, TreeMap<Integer, LinkedList<RawDataCrawledCell>>> tempMap = rawCellMap.entrySet()
                    .stream()
                    .collect(Collectors.groupingBy(
                            entry -> entry.getKey().getTable(),
                            TreeMap::new,
                            Collectors.groupingBy(
                                    entry2 -> entry2.getKey().getRow(),
                                    TreeMap::new,
                                    Collectors.collectingAndThen(
                                            Collectors.mapping(
                                                    Map.Entry::getValue,
                                                    Collectors.toCollection(LinkedList::new)
                                            ),
                                            Function.identity()
                                    )
                            )
                    ));
            logger.info("rawCellMap group by table and by row as temp map");

            //merge spanFixMap data
            for (Map.Entry<RawDataCrawledCoord, RawDataCrawledCell> entry : spanFixMap.entrySet()) {
                RawDataCrawledCoord coord = entry.getKey();
                RawDataCrawledCell cell = entry.getValue();

                int table = coord.getTable();
                int row = coord.getRow();
                int column = coord.getColumn();

                //insert fix cell to map
                tempMap.get(table).get(row).add(column - 1, cell);
            }
            logger.info("insert spanFixMap element into temp map");

            //to result-map
            for (Map.Entry<Integer, TreeMap<Integer, LinkedList<RawDataCrawledCell>>> entry : tempMap.entrySet()) {
                Integer table = entry.getKey();
                TreeMap<Integer, LinkedList<RawDataCrawledCell>> rowAndCells = entry.getValue();

                for (Map.Entry<Integer, LinkedList<RawDataCrawledCell>> entry2 : rowAndCells.entrySet()) {
                    Integer row = entry2.getKey();
                    LinkedList<RawDataCrawledCell> cellChain = entry2.getValue();
                    Optional<RawDataCrawledCoord> coordOptional = rawCellMap.keySet()
                            .stream()
                            .filter(coord -> coord.getTable() == table && coord.getRow() == row)
                            .findFirst();
                    Assert.isTrue(coordOptional.isPresent(), "first column of row should be exist");
                    RawDataCrawledCoord coord = coordOptional.get();
                    for (RawDataCrawledCell cell : cellChain) {
                        resultMap.put(coord, cell);
                        coord = coord.cloneAndNextColumn();
                    }
                }
            }
            logger.info("turn temp map into result...done");
            return resultMap;
        }

    }


}
