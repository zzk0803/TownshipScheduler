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

    public ParsedResult process(CrawledResult crawledResult) {
        logger.info("ParsedResult working...");
        LinkedHashMap<String, ParsedResultSegment> result
                = new LinkedHashMap<>();
        TreeMap<CrawledDataCoordinate, CrawledDataCell> coordinateToDataCellMap
                = crawledResult.crawledDataCellTreeMap();

        LinkedList<CrawledDataCoordinate> oneCoordRowAsHeadRowList
                = filterCoordTypeHeadAsList(coordinateToDataCellMap);

        int headRowSize = oneCoordRowAsHeadRowList.size();
        Assert.isTrue(headRowSize >= 2, "headRowSize should be more than 2");

        Iterator<CrawledDataCoordinate> rowIterator = oneCoordRowAsHeadRowList.iterator();
        CrawledDataCoordinate formerRowCoord = null;
        CrawledDataCoordinate latterRowCoord = null;
        if (rowIterator.hasNext()) {
            formerRowCoord = rowIterator.next();
        }
        if (rowIterator.hasNext()) {
            latterRowCoord = rowIterator.next();
        } else {
            logger.error("headRow should be more than two");
            throw new IllegalStateException("FIX ME");
        }

        boolean rowIteratorBool = true;
        while (rowIteratorBool) {
            SortedMap<CrawledDataCoordinate, CrawledDataCell> betweenTwoHeadersAsSortedMap
                    = coordinateToDataCellMap.subMap(
                    formerRowCoord,
                    false,
                    latterRowCoord,
                    false
            );

            if (betweenTwoHeadersAsSortedMap.isEmpty()) {
                formerRowCoord = latterRowCoord;
                if (rowIterator.hasNext()) {
                    latterRowCoord = rowIterator.next();
                } else {
                    rowIteratorBool = false;
                }
                continue;
            }

            TreeMap<Integer, List<Map.Entry<CrawledDataCoordinate, CrawledDataCell>>> betweenTwoHeader
                    = betweenTowHeaderAndGroupByRowNumber(betweenTwoHeadersAsSortedMap);

            String categotyString = figureOutCategotyString(
                    formerRowCoord,
                    oneCoordRowAsHeadRowList,
                    coordinateToDataCellMap
            );

            Map.Entry<Integer, List<Map.Entry<CrawledDataCoordinate, CrawledDataCell>>> columnsNameRowAsMapEntry
                    = betweenTwoHeader.firstEntry();

            SortedMap<Integer, List<Map.Entry<CrawledDataCoordinate, CrawledDataCell>>> dataAsMapEntries
                    = betweenTwoHeader.tailMap(
                    columnsNameRowAsMapEntry.getKey(),
                    false
            );

            ParsedResultSegment parsedResultSegment = new ParsedResultSegment(categotyString);
            for (Map.Entry<CrawledDataCoordinate, CrawledDataCell> columnEntry : columnsNameRowAsMapEntry.getValue()) {
                AtomicInteger rowCounter = new AtomicInteger(0);
                dataAsMapEntries.values()
                        .stream()
                        .flatMap(Collection::stream)
                        .filter(dataEntry -> dataEntry.getKey().getTable() == columnEntry.getKey().getTable())
                        .filter(dataEntry -> dataEntry.getKey().getColumn() == columnEntry.getKey().getColumn())
                        .forEach(dataEntry -> {
                            String columnName = columnEntry.getValue().reasonableText();
                            CrawledDataCell crawledDataCell = dataEntry.getValue();
                            parsedResultSegment.add(rowCounter.incrementAndGet(), columnName, crawledDataCell);
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

    private TreeMap<Integer, List<Map.Entry<CrawledDataCoordinate, CrawledDataCell>>> betweenTowHeaderAndGroupByRowNumber(
            SortedMap<CrawledDataCoordinate, CrawledDataCell> betweenTwoHeaders
    ) {
        return betweenTwoHeaders.entrySet()
                .stream()
                .collect(Collectors.groupingBy(
                                entry -> entry.getKey().getRow(),
                                TreeMap::new,
                                Collectors.toList()
                        )
                );
    }

    private LinkedList<CrawledDataCoordinate> filterCoordTypeHeadAsList(TreeMap<CrawledDataCoordinate, CrawledDataCell> crawledAsMap) {
        return crawledAsMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getType() == CrawledDataCell.Type.HEAD)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private String figureOutCategotyString(
            CrawledDataCoordinate currentRowCoord,
            LinkedList<CrawledDataCoordinate> headRowCoordList,
            TreeMap<CrawledDataCoordinate, CrawledDataCell> fixedCellMap
    ) {
        String result = "n/a";

        CrawledDataCell currentCell = fixedCellMap.get(currentRowCoord);
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
                    CrawledDataCoordinate previousCoord = headRowCoordList.get(currentIdx - 1);
                    return figureOutCategotyString(previousCoord, headRowCoordList, fixedCellMap);
                }
            }
        }

        return result;
    }


}
