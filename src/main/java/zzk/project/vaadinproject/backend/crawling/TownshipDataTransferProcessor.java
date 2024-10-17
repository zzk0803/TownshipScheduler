package zzk.project.vaadinproject.backend.crawling;

import com.google.common.collect.Table;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zzk.project.vaadinproject.backend.persistence.Goods;
import zzk.project.vaadinproject.backend.persistence.TownshipCrawledRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
class TownshipDataTransferProcessor {

    public static final Logger logger = LoggerFactory.getLogger(TownshipDataTransferProcessor.class);

    private final TownshipCrawledRepository townshipCrawledRepository;


    public TransferResult process(ParsedResult parsedResult) {
        logger.info("parsing into entity ...");

        List<Goods> goodsArrayList = new ArrayList<>();

        Map<String, ParsedResultSegment> resultGroupByTable = parsedResult.groupByTable();
        Set<Map.Entry<String, ParsedResultSegment>> entries = resultGroupByTable.entrySet();
        for (Map.Entry<String, ParsedResultSegment> entry : entries) {
            ParsedResultSegment parsedResultSegment = entry.getValue();
            String category = parsedResultSegment.getCategory();

            int size = parsedResultSegment.size();
            Table<Integer, String, RawDataCrawledCell> cellTable = parsedResultSegment.getTable();
//            MultiValueMap<String, RawDataCrawledCell> columnCellsMultiValueMap = parsedResultSegment.getColumnValuesMap();
            Set<String> columnNames = cellTable.columnKeySet();
//            Set<String> columnNames = columnCellsMultiValueMap.keySet();

            cellTable.rowMap().forEach((rowIndex, rowEntry) -> {
                Goods goods = new Goods();
                goods.setCategory(category);

                rowEntry.forEach((columnName, columnCell) -> {
                    Scanner cellContentScanner = new Scanner(columnCell.reasonableText());
                    switch (columnName.toLowerCase()) {
                        case "goods" -> {
                            Pattern pattern = Pattern.compile("x\\d");
                            Matcher matcher = pattern.matcher(cellContentScanner.nextLine());
                            String removeXnPtn = matcher.replaceAll("");
                            goods.setName(removeXnPtn);
                        }
                        case "goods[colspan:1]", "Goods[colspan:1]" -> {//include symbol '[' ']',so str.tolowercase() doesn't work??
                            columnCell.getImageString().ifPresent(imgUrl -> {
                                goods.setImageBytes(
                                        townshipCrawledRepository.queryImageBytesByHtml(imgUrl)
                                );
                            });
                        }
                        case "image" -> {
                            columnCell.getImageString().ifPresent(imgUrl -> {
                                goods.setImageBytes(
                                        townshipCrawledRepository.queryImageBytesByHtml(imgUrl)
                                );
                            });

                        }
                        case "materials" -> {
                            if (cellContentScanner.hasNextLine()) {
                                goods.setBomString(cellContentScanner.nextLine());
                            }
                        }
                        case "level" -> {
                            if (cellContentScanner.hasNextInt()) {
                                goods.setLevel(cellContentScanner.nextInt());
                            }
                        }
                        case "cost" -> {
                            if (cellContentScanner.hasNextInt()) {
                                goods.setCost(cellContentScanner.nextInt());
                            }
                        }
                        case "sell price", "price" -> {
                            if (cellContentScanner.hasNextInt()) {
                                goods.setSellPrice(cellContentScanner.nextInt());
                            }
                        }
                        case "xp" -> {
                            if (cellContentScanner.hasNextInt()) {
                                goods.setXp(cellContentScanner.nextInt());
                            }
                        }
                        case "time 0%-0", "Time 0%-0[colspan:1]", "time 0%-0[colspan:1]" -> {
                            if (cellContentScanner.hasNextLine()) {
                                String removedSpanHint = cellContentScanner.nextLine();
                                goods.setDurationString(removedSpanHint);
                            }
                        }
                        case "dealer available icon", "Dealer Available Icon[colspan:1]",
                             "dealer available icon[colspan:1]" -> {
                            if (cellContentScanner.hasNextInt()) {
                                goods.setDealerValue(cellContentScanner.nextInt());
                            }
                        }
                        case "help icon", "Help Icon[colspan:1]", "help icon[colspan:1]" -> {
                            if (cellContentScanner.hasNextInt()) {
                                goods.setHelpValue(cellContentScanner.nextInt());
                            }
                        }
                        default -> {
                            logger.warn("{}={} is resolve confused", columnName, cellContentScanner.nextLine());
                        }
                    }
                    goodsArrayList.add(goods);
                });
            });
//            for (int itemIdx = 0; itemIdx < alignSize; itemIdx++) {
//                Goods goods = new Goods();
//                goods.setCategory(category);
//
//                Scanner cellContentScanner;
//                for (String columnName : columnNames) {
//                    List<RawDataCrawledCell> cells = columnCellsMultiValueMap.get(columnName);
//                    RawDataCrawledCell cell = cells.get(itemIdx);
//                    String reasonableText = cell.reasonableText();
//                    cellContentScanner = new Scanner(reasonableText);
//                    String columnNameLowerCase = columnName.toLowerCase();
//                    switch (columnNameLowerCase) {
//                        case "goods" -> {
//                            Pattern pattern = Pattern.compile("x\\d");
//                            Matcher matcher = pattern.matcher(cellContentScanner.nextLine());
//                            String removeXnPtn = matcher.replaceAll("");
//                            goods.setName(removeXnPtn);
//                        }
//                        case "goods[colspan:1]", "Goods[colspan:1]" -> {//include symbol '[' ']',so str.tolowercase() doesn't work??
//                            cell.getImageString().ifPresent(imgUrl -> {
//                                goods.setImageBytes(
//                                        townshipCrawledRepository.queryImageBytesByHtml(imgUrl)
//                                );
//                            });
//                        }
//                        case "image" -> {
//                            cell.getImageString().ifPresent(imgUrl -> {
//                                goods.setImageBytes(
//                                        townshipCrawledRepository.queryImageBytesByHtml(imgUrl)
//                                );
//                            });
//
//                        }
//                        case "materials" -> {
//                            goods.setBomString(cellContentScanner.nextLine());
//                        }
//                        case "level" -> {
//                            goods.setLevel(cellContentScanner.nextInt());
//                        }
//                        case "cost" -> {
//                            goods.setCost(cellContentScanner.nextInt());
//                        }
//                        case "sell price", "price" -> {
//                            goods.setSellPrice(cellContentScanner.nextInt());
//                        }
//                        case "xp" -> {
//                            goods.setXp(cellContentScanner.nextInt());
//                        }
//                        case "time 0%-0", "Time 0%-0[colspan:1]", "time 0%-0[colspan:1]" -> {
//                            String removedSpanHint = cellContentScanner.nextLine().replace("[colspan:1]", "");
//                            goods.setDurationString(removedSpanHint);
//                        }
//                        case "dealer available icon", "Dealer Available Icon[colspan:1]",
//                             "dealer available icon[colspan:1]" -> {
//                            goods.setDealerValue(cellContentScanner.nextInt());
//                        }
//                        case "help icon", "Help Icon[colspan:1]", "help icon[colspan:1]" -> {
//                            goods.setHelpValue(cellContentScanner.nextInt());
//                        }
//                        default -> {
//                            logger.warn("{}={} is resolve confused", columnName, reasonableText);
//                        }
//                    }
//
//                }
//
//                goodsArrayList.add(goods);
//            }

        }

        logger.info("parse into entity done");
        return new TransferResult(goodsArrayList);
    }


}
