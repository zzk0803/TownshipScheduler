package zzk.townshipscheduler.backend.crawling;

import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor
class TownshipDataParsingProcessor {

    public static final Logger logger = LoggerFactory.getLogger(TownshipDataParsingProcessor.class);

    private static TreeMap<Integer, List<Map.Entry<RawDataCrawledCoord, RawDataCrawledCell>>> betweenTowHeaderAndGroupByRowNumber(
            SortedMap<RawDataCrawledCoord, RawDataCrawledCell> betweenTwoHeaders
    ) {
        return betweenTwoHeaders.entrySet()
                .stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().getRow(),
                        TreeMap::new,
                        Collectors.toList()
                ));
    }

    public ParsedResult process(CrawledResult crawledResult) {
        logger.info("ParsedResult working...");
        LinkedHashMap<String, ParsedResultSegment> result = new LinkedHashMap<>();
        TreeMap<RawDataCrawledCoord, RawDataCrawledCell> crawledAsMap = crawledResult.get();

        LinkedList<RawDataCrawledCoord> headRowCoordList = filterCoordTypeHeadAsList(crawledAsMap);

        int headRowSize = headRowCoordList.size();
        Assert.isTrue(headRowSize >= 2, "should be more than 2");

        Iterator<RawDataCrawledCoord> rowIterator = headRowCoordList.iterator();
        RawDataCrawledCoord formerRowCoord = null;
        RawDataCrawledCoord latterRowCoord = null;
        if (rowIterator.hasNext()) {
            formerRowCoord = rowIterator.next();
        }
        if (rowIterator.hasNext()) {
            latterRowCoord = rowIterator.next();
        } else {
            logger.error("should be more than two");
            throw new IllegalStateException("FIX ME");
        }

        boolean rowIteratorBool = true;
        while (rowIteratorBool) {
            SortedMap<RawDataCrawledCoord, RawDataCrawledCell> betweenTwoHeaders = crawledAsMap.subMap(
                    formerRowCoord,
                    false,
                    latterRowCoord,
                    false
            );

            if (betweenTwoHeaders.isEmpty()) {
                formerRowCoord = latterRowCoord;
                if (rowIterator.hasNext()) {
                    latterRowCoord = rowIterator.next();
                } else {
                    rowIteratorBool = false;
                }
                continue;
            }

            TreeMap<Integer, List<Map.Entry<RawDataCrawledCoord, RawDataCrawledCell>>> betweenTwoHeadersGroupByRow =
                    betweenTowHeaderAndGroupByRowNumber(betweenTwoHeaders);

            String categotyString = figureOutCategotyString(formerRowCoord, headRowCoordList, crawledAsMap);

            Map.Entry<Integer, List<Map.Entry<RawDataCrawledCoord, RawDataCrawledCell>>> columnsNameRowAsMapEntry =
                    betweenTwoHeadersGroupByRow.firstEntry();

            SortedMap<Integer, List<Map.Entry<RawDataCrawledCoord, RawDataCrawledCell>>> dataAsMapEntries = betweenTwoHeadersGroupByRow.tailMap(
                    columnsNameRowAsMapEntry.getKey(),
                    false
            );

            ParsedResultSegment parsedResultSegment = new ParsedResultSegment(categotyString);
            for (Map.Entry<RawDataCrawledCoord, RawDataCrawledCell> columnEntry : columnsNameRowAsMapEntry.getValue()) {
                AtomicInteger rowCounter = new AtomicInteger(0);
                dataAsMapEntries.values().stream()
                        .flatMap(Collection::stream)
                        .filter(dataEntry -> dataEntry.getKey().getTable() == columnEntry.getKey().getTable())
                        .filter(dataEntry -> dataEntry.getKey().getColumn() == columnEntry.getKey().getColumn())
                        .forEach(dataEntry -> {
                            String columnName = columnEntry.getValue().reasonableText();
                            RawDataCrawledCell rawDataCrawledCell = dataEntry.getValue();
//                            parsedResultSegment.add(columnName, rawDataCrawledCell);
                            parsedResultSegment.add(rowCounter.incrementAndGet(), columnName, rawDataCrawledCell);
                        });
            }
            result.put(categotyString, parsedResultSegment);

            formerRowCoord = latterRowCoord;
            if (rowIterator.hasNext()) {
                latterRowCoord = rowIterator.next();
            } else {
                rowIteratorBool = false;
            }
        }

        logger.info("ParsedResult done");
        return new ParsedResult(result);
    }

    private LinkedList<RawDataCrawledCoord> filterCoordTypeHeadAsList(TreeMap<RawDataCrawledCoord, RawDataCrawledCell> crawledAsMap) {
        return crawledAsMap.entrySet().stream()
                .filter(entry -> entry.getValue().getType() == RawDataCrawledCell.Type.HEAD)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private String figureOutCategotyString(
            RawDataCrawledCoord currentRowCoord,
            LinkedList<RawDataCrawledCoord> headRowCoordList,
            TreeMap<RawDataCrawledCoord, RawDataCrawledCell> fixedCellMap
    ) {
        String result = "n/a";

        RawDataCrawledCell currentCell = fixedCellMap.get(currentRowCoord);
        String currentText = currentCell.getText();

        boolean cellCategoryJudge = currentCell.boolContentLooksLikeCategory();

        if (currentRowCoord.getRow() == 1) {
            result = currentText;
        } else {
            if (cellCategoryJudge) {
                result = currentText;
            } else {
                int currentIdx = headRowCoordList.indexOf(currentRowCoord);
                if (currentIdx > 0) {
                    RawDataCrawledCoord previousCoord = headRowCoordList.get(currentIdx - 1);
                    return figureOutCategotyString(previousCoord, headRowCoordList, fixedCellMap);
                }
            }
        }

        return result;
    }


}
