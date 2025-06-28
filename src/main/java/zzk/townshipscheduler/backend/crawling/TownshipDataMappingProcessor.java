package zzk.townshipscheduler.backend.crawling;

import com.google.common.collect.Table;
import lombok.RequiredArgsConstructor;
import org.atteo.evo.inflector.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.dao.WikiCrawledEntityRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
class TownshipDataMappingProcessor {

    public static final Logger logger = LoggerFactory.getLogger(TownshipDataMappingProcessor.class);

    public static final Pattern PRODUCT_NAME_GAIN_AMOUNT_PATTERN = Pattern.compile("^(.+?)\\s*x(\\d+)$");

    private final WikiCrawledEntityRepository wikiCrawledEntityRepository;

    public TransferResult process(ParsedResult parsedResult) {
        logger.info("parsing into entity ...");

        List<ProductEntity> productEntityArrayList = new ArrayList<>();

        Map<String, ParsedResultSegment> resultGroupByTable = parsedResult.groupByTable();
        Set<Map.Entry<String, ParsedResultSegment>> entries = resultGroupByTable.entrySet();
        for (Map.Entry<String, ParsedResultSegment> entry : entries) {
            ParsedResultSegment parsedResultSegment = entry.getValue();
            String category = parsedResultSegment.getCategory();

            int size = parsedResultSegment.size();
            Table<Integer, String, CrawledDataCell> cellTable = parsedResultSegment.getTable();
//            MultiValueMap<String, RawDataCrawledCell> columnCellsMultiValueMap = parsedResultSegment.getColumnValuesMap();
            Set<String> columnNames = cellTable.columnKeySet();
//            Set<String> columnNames = columnCellsMultiValueMap.keySet();

            Map<Integer, Map<String, CrawledDataCell>> rowNumberMapColumnDataMap = cellTable.rowMap();
            rowNumberMapColumnDataMap.forEach((keyAsRow, valueAsColumnDataMap) -> {
                ProductEntity productEntity = new ProductEntity();
                productEntity.setCategory(category);

                valueAsColumnDataMap.forEach((keyAsColumn, valueAsDataCell) -> {
                    Scanner cellContentScanner = new Scanner(valueAsDataCell.reasonableText());
                    switch (keyAsColumn.toLowerCase()) {
                        case "goods" -> {
                            if (cellContentScanner.hasNextLine()) {
                                String mayMixNameAndGain = cellContentScanner.nextLine();
                                Matcher nameGainMatcher = PRODUCT_NAME_GAIN_AMOUNT_PATTERN.matcher(mayMixNameAndGain);
                                if (nameGainMatcher.find()) {
                                    // 如果匹配成功，提取单词和数字
                                    String productName = nameGainMatcher.group(1).trim();
                                    productEntity.setName(productName);
                                    productEntity.setNameForMaterial(English.plural(productName.toLowerCase(),1));
                                    String number = nameGainMatcher.group(2);
                                    productEntity.setGainWhenCompleted(Integer.parseInt(number));
                                } else {
                                    String productName = mayMixNameAndGain;
                                    productEntity.setName(productName);
                                    productEntity.setNameForMaterial(English.plural(productName.toLowerCase(),1));
                                }
                            }
                        }
                        case "goods[colspan:1]",
                             "Goods[colspan:1]" -> {//include symbol '[' ']',so str.tolowercase() doesn't work??
                            valueAsDataCell.getImageString().ifPresent(imgUrl -> {
//                                productEntity.setImageBytes(
//                                        wikiCrawledEntityRepository.queryImageBytesByHtml(imgUrl)
//                                );
                                productEntity.setCrawledAsImage(wikiCrawledEntityRepository.queryEntityBearImageByHtml(imgUrl));
                            });
                        }
                        case "image" -> {
                            valueAsDataCell.getImageString().ifPresent(imgUrl -> {
//                                productEntity.setImageBytes(
//                                        wikiCrawledEntityRepository.queryImageBytesByHtml(imgUrl)
//                                );
                                productEntity.setCrawledAsImage(wikiCrawledEntityRepository.queryEntityBearImageByHtml(imgUrl));
                            });

                        }
                        case "materials" -> {
                            if (cellContentScanner.hasNextLine()) {
                                productEntity.setBomString(cellContentScanner.nextLine().toLowerCase());
                            }
                        }
                        case "level" -> {
                            if (cellContentScanner.hasNextInt()) {
                                productEntity.setLevel(cellContentScanner.nextInt());
                            }
                        }
                        case "cost" -> {
                            if (cellContentScanner.hasNextInt()) {
                                productEntity.setCost(cellContentScanner.nextInt());
                            }
                        }
                        case "sell price", "price" -> {
                            if (cellContentScanner.hasNextInt()) {
                                productEntity.setSellPrice(cellContentScanner.nextInt());
                            }
                        }
                        case "xp" -> {
                            if (cellContentScanner.hasNextInt()) {
                                productEntity.setXp(cellContentScanner.nextInt());
                            }
                        }
                        case "time 0%-0", "Time 0%-0[colspan:1]", "time 0%-0[colspan:1]" -> {
                            if (cellContentScanner.hasNextLine()) {
                                String removedSpanHint = cellContentScanner.nextLine();
                                productEntity.setDurationString(removedSpanHint);
                            }
                        }
                        case "dealer available icon", "Dealer Available Icon[colspan:1]",
                             "dealer available icon[colspan:1]" -> {
                            if (cellContentScanner.hasNextInt()) {
                                productEntity.setDealerValue(cellContentScanner.nextInt());
                            }
                        }
                        case "help icon", "Help Icon[colspan:1]", "help icon[colspan:1]" -> {
                            if (cellContentScanner.hasNextInt()) {
                                productEntity.setHelpValue(cellContentScanner.nextInt());
                            }
                        }
                        default -> {
                            logger.warn("{}={} is resolve confused", keyAsColumn, cellContentScanner.nextLine());
                        }
                    }
                });
                productEntityArrayList.add(productEntity);
            });

        }

        logger.info("parse into entity done");
        return new TransferResult(productEntityArrayList);
    }


}
