package zzk.townshipscheduler.backend.crawling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CrawledDataMemory {

    private static final Logger logger = LoggerFactory.getLogger(CrawledDataMemory.class);

    private final TreeMap<CrawledDataCoordinate, CrawledDataCell> rawCellMap = new TreeMap<>();

    private final TreeMap<CrawledDataCoordinate, CrawledDataCell> spanFixMap = new TreeMap<>();

    CrawledResult completeAndMend() {
        logger.info("append SENTINEL pair");

        //because parse algorithm take window between two head row(which column size suppose to be one),
        // last dataZone miss the row doesn't exist
        //so append a row  called "sentinel" make the algorithm make it work
        putForSave(
                new CrawledDataCoordinate(
                        "SENTINEL",
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                ),
                new CrawledDataCell(
                        "SENTINEL",
                        "SENTINEL",
                        Collections.emptyList(),
                        Collections.emptyList(),
                        CrawledDataCell.Type.HEAD,
                        new CrawledDataCell.CellSpan(0, 0)
                )
        );

        MendProgress mendProgress = new MendProgress(rawCellMap, spanFixMap, new TreeMap<>());
        TreeMap<CrawledDataCoordinate, CrawledDataCell> processed = mendProgress.process();

        return new CrawledResult(processed);
    }

    void putForSave(CrawledDataCoordinate coord, CrawledDataCell cell) {
        rawCellMap.put(coord, cell);
    }

    void reset() {
        rawCellMap.clear();
        spanFixMap.clear();
    }

    void putForMend(CrawledDataCoordinate coord, CrawledDataCell cell) {
        spanFixMap.put(coord, cell);
    }

    private static class MendProgress {

        private final TreeMap<CrawledDataCoordinate, CrawledDataCell> rawCellMap;

        private final TreeMap<CrawledDataCoordinate, CrawledDataCell> spanFixMap;

        private final TreeMap<CrawledDataCoordinate, CrawledDataCell> resultMap;

        public MendProgress(
                TreeMap<CrawledDataCoordinate, CrawledDataCell> rawCellMap,
                TreeMap<CrawledDataCoordinate, CrawledDataCell> spanFixMap,
                TreeMap<CrawledDataCoordinate, CrawledDataCell> resultMap
        ) {
            this.rawCellMap = rawCellMap;
            this.spanFixMap = spanFixMap;
            this.resultMap = resultMap;
        }

        private TreeMap<CrawledDataCoordinate, CrawledDataCell> result() {
            return this.resultMap;
        }

        private TreeMap<CrawledDataCoordinate, CrawledDataCell> process() {
            logger.info("do mending...");
            //<table,<row,ProductsCrawledCell>>
            TreeMap<Integer, TreeMap<Integer, LinkedList<CrawledDataCell>>> tempMap = rawCellMap.entrySet()
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
            for (Map.Entry<CrawledDataCoordinate, CrawledDataCell> entry : spanFixMap.entrySet()) {
                CrawledDataCoordinate coord = entry.getKey();
                CrawledDataCell cell = entry.getValue();

                int table = coord.getTable();
                int row = coord.getRow();
                int column = coord.getColumn();

                //insert fix cell to map
                tempMap.get(table).get(row).add(column - 1, cell);
            }
            logger.info("insert spanFixMap element into temp map");

            //to result-map
            for (Map.Entry<Integer, TreeMap<Integer, LinkedList<CrawledDataCell>>> entry : tempMap.entrySet()) {
                Integer table = entry.getKey();
                TreeMap<Integer, LinkedList<CrawledDataCell>> rowAndCells = entry.getValue();

                for (Map.Entry<Integer, LinkedList<CrawledDataCell>> entry2 : rowAndCells.entrySet()) {
                    Integer row = entry2.getKey();
                    LinkedList<CrawledDataCell> cellChain = entry2.getValue();
                    Optional<CrawledDataCoordinate> coordOptional = rawCellMap.keySet()
                            .stream()
                            .filter(coord -> coord.getTable() == table && coord.getRow() == row)
                            .findFirst();
                    Assert.isTrue(coordOptional.isPresent(), "first column of row should be exist");
                    CrawledDataCoordinate coord = coordOptional.get();
                    for (CrawledDataCell cell : cellChain) {
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
