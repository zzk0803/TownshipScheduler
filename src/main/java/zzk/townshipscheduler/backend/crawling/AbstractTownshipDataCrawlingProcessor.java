package zzk.townshipscheduler.backend.crawling;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledParsedCoordCellEntityRepository;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public abstract class AbstractTownshipDataCrawlingProcessor {

    public static final String[] ABANDON_ZONE = {"Gems", "Construction Materials"};

    protected final Set<CrawledDataCell.Img> imageToDownload;

    protected final CrawledDataMemory crawledDataMemory;

    protected final WikiCrawledEntityRepository wikiCrawledEntityRepository;

    protected final WikiCrawledParsedCoordCellEntityRepository wikiCrawledParsedCoordCellEntityRepository;

    protected final ExecutorService townshipExecutorService;

    protected final TransactionTemplate transactionTemplate;

    public AbstractTownshipDataCrawlingProcessor(
            CrawledDataMemory crawledDataMemory,
            WikiCrawledEntityRepository wikiCrawledEntityRepository,
            WikiCrawledParsedCoordCellEntityRepository wikiCrawledParsedCoordCellEntityRepository,
            ExecutorService townshipExecutorService,
            TransactionTemplate transactionTemplate
    ) {
        this.imageToDownload = new LinkedHashSet<>();
        this.crawledDataMemory = crawledDataMemory;
        this.wikiCrawledEntityRepository = wikiCrawledEntityRepository;
        this.wikiCrawledParsedCoordCellEntityRepository = wikiCrawledParsedCoordCellEntityRepository;
        this.townshipExecutorService = townshipExecutorService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Common logic for processing table elements.
     *
     * @param articleTableElements The table elements to process
     * @return CompletableFuture with CrawledResult
     */
    protected CompletableFuture<CrawledResult> doProcessTables(Elements articleTableElements) {
        for (int i = 0; i < articleTableElements.size(); i++) {
            int tableNum = 1 + i;

            Element currentTable = articleTableElements.get(i);
            String tableZoneString = findTableZoneString(currentTable);
            if (checkAbandonZone(tableZoneString)) {
                continue;
            }

            doTableParse(
                    currentTable,
                    tableNum,
                    tableZoneString
            );
        }

        return CompletableFuture.supplyAsync(
                crawledDataMemory::completeAndMend,
                townshipExecutorService
        );
    }

    private String findTableZoneString(Element currentTable) {
        Optional<String> tableZone = currentTable.parents()
                .stream()
                .filter(element -> element.hasClass("mw-collapsible-content"))
                .findFirst()
                .map(element -> {
                    Element mwHeadlineElement = element.previousElementSibling();
                    return Objects.isNull(mwHeadlineElement)
                            ? ""
                            : mwHeadlineElement.select("span.mw-headline")
                                    .first()
                                    .text();
                });
        return tableZone.orElse("");
    }

    private boolean checkAbandonZone(String tableZoneString) {
        return Arrays.stream(ABANDON_ZONE)
                .anyMatch(tableZoneString::equalsIgnoreCase);
    }

    private void doTableParse(
            Element currentTable,
            int tableNum,
            String tableZoneString
    ) {
        Elements trElements = currentTable.getElementsByTag("tr");
        int currentRowsSize = trElements.size();

        CrawledDataCoordinate crawledCoordPrototype = CrawledDataCoordinate.create();
        //row iteration
        for (int currentRowNum = 0; currentRowNum < currentRowsSize; currentRowNum++) {
            Element currentRow = trElements.get(currentRowNum);
            Elements trChildrenElements = currentRow.children();
            int currentColumnsSize = trChildrenElements.size();

            crawledCoordPrototype = crawledCoordPrototype.cloneAndNextRow();
            //column iteration
            for (int currentColumnNum = 0; currentColumnNum < currentColumnsSize; currentColumnNum++) {
                Element currentThOrTd = trChildrenElements.get(currentColumnNum);

                String html = doParseUnitIntoHtml(currentThOrTd);
                String text = doParseUnitIntoText(currentThOrTd);
                List<CrawledDataCell.Anchor> anchorList = doParseUnitIntoAnchor(currentThOrTd);
                List<CrawledDataCell.Img> imgList = doParseUnitIntoImg(currentThOrTd);
                imageToDownload.addAll(imgList);

                int rowSpan = doParseUnitIntoRowSpanInt(currentThOrTd);
                int colSpan = doParseUnitIntoColSpanInt(currentThOrTd);

                crawledCoordPrototype = crawledCoordPrototype.cloneAndNextColumn();

                CrawledDataCoordinate currentCoord = crawledCoordPrototype.clone();
                currentCoord.setTable(tableNum);
                currentCoord.setTableZone(tableZoneString);

                CrawledDataCell currentCell = CrawledDataCell.builder()
                        .html(html)
                        .text(text)
                        .anchorList(anchorList)
                        .imgList(imgList)
                        .type(currentColumnsSize == 1
                                ? CrawledDataCell.Type.HEAD
                                : CrawledDataCell.Type.CELL)
                        .span(new CrawledDataCell.CellSpan(
                                rowSpan,
                                colSpan
                        ))
                        .build();

                registerSpanFixIfNeed(
                        currentCoord,
                        currentCell,
                        rowSpan,
                        colSpan
                );

                putIntoMemory(
                        currentCoord,
                        currentCell
                );
            }
            crawledCoordPrototype = crawledCoordPrototype.cloneAndResetColumn();
        }
    }

    private String doParseUnitIntoHtml(Element currentThOrTd) {
        return currentThOrTd.html();
    }

    private String doParseUnitIntoText(Element currentThOrTd) {
        return currentThOrTd.text();
    }

    private List<CrawledDataCell.Anchor> doParseUnitIntoAnchor(Element currentThOrTd) {
        Elements anchorElements = currentThOrTd.select("a");
        return anchorElements.stream()
                .map(element -> {
                    CrawledDataCell.Anchor l = new CrawledDataCell.Anchor();
                    l.setHref(element.attr("href"));
                    l.setTitle(element.attr("title"));
                    l.setText(element.text());
                    return l;
                })
                .toList();
    }

    private List<CrawledDataCell.Img> doParseUnitIntoImg(Element currentThOrTd) {
        Elements imageElements = currentThOrTd.select("img");
        return imageElements.stream()
                .map(element -> {
                    CrawledDataCell.Img cellImg = new CrawledDataCell.Img();
                    cellImg.setAlt(element.attr("alt"));
                    cellImg.setSrc(element.attr("data-src"));
                    return cellImg;
                })
                .toList();
    }

    private int doParseUnitIntoRowSpanInt(Element currentThOrTd) {
        String rowspan = currentThOrTd.attr("rowspan");
        return rowspan.isBlank()
                ? 0
                : Integer.parseInt(rowspan);
    }

    private int doParseUnitIntoColSpanInt(Element currentThOrTd) {
        String colspan = currentThOrTd.attr("colspan");
        return colspan.isBlank()
                ? 0
                : Integer.parseInt(colspan);
    }

    private void registerSpanFixIfNeed(
            CrawledDataCoordinate currentCoordinate,
            CrawledDataCell currentCell,
            int rowSpan,
            int colSpan
    ) {
        if (currentCell.getType() == CrawledDataCell.Type.CELL) {
            registerRowSpanFix(
                    rowSpan,
                    currentCell,
                    currentCoordinate
            );
            registerColSpanFix(
                    colSpan,
                    currentCell,
                    currentCoordinate
            );
        }
    }

    private void registerRowSpanFix(
            int rowSpan,
            CrawledDataCell currentCell,
            CrawledDataCoordinate currentCoordinate
    ) {
        if (rowSpan > CrawledDataCell.CellSpan.NA_EFFECT) {
            CrawledDataCell.CellSpan fixedSpan = new CrawledDataCell.CellSpan(
                    CrawledDataCell.CellSpan.REGULAR,
                    CrawledDataCell.CellSpan.REGULAR
            );
            CrawledDataCell cellFixed = currentCell.clone();
            cellFixed.setSpan(fixedSpan);

            int fixSize = rowSpan - 1;
            CrawledDataCoordinate mendedCoord = currentCoordinate.cloneAndNextRow();
            while (fixSize > 0) {
                hintIntoMemory(
                        mendedCoord,
                        cellFixed
                );
                fixSize -= 1;
                mendedCoord = mendedCoord.cloneAndNextRow();
            }
        }
    }

    private void hintIntoMemory(
            CrawledDataCoordinate currentCoordinate,
            CrawledDataCell fixCell
    ) {
        crawledDataMemory.putForMend(
                currentCoordinate,
                fixCell
        );
    }

    private void registerColSpanFix(
            int colSpan,
            CrawledDataCell currentCell,
            CrawledDataCoordinate currentCoordinate
    ) {
        if (colSpan > CrawledDataCell.CellSpan.NA_EFFECT) {
            CrawledDataCell.CellSpan fixedSpan = new CrawledDataCell.CellSpan(
                    CrawledDataCell.CellSpan.REGULAR,
                    CrawledDataCell.CellSpan.REGULAR
            );
            CrawledDataCell cellFixed = currentCell.clone();
            cellFixed.setSpan(fixedSpan);

            int fixSize = colSpan - 1;
            for (int i = 0; i < fixSize; i++) {
                CrawledDataCell fixCellToSave = cellFixed.clone();
                fixCellToSave.setText(fixCellToSave.reasonableText() + "[colspan:" + (i + 1) + "]");
                CrawledDataCoordinate mendedCoord = currentCoordinate.cloneAndNextColumn();
                hintIntoMemory(
                        mendedCoord,
                        fixCellToSave
                );
            }
        }
    }

    private void putIntoMemory(
            CrawledDataCoordinate currentCoordinate,
            CrawledDataCell currentCell
    ) {
        crawledDataMemory.putForSave(
                currentCoordinate,
                currentCell
        );
    }

    protected void persistDocument(Document document) {
        WikiCrawledEntity wikiCrawledEntity = new WikiCrawledEntity();
        wikiCrawledEntity.setType(WikiCrawledEntity.Type.HTML);
        wikiCrawledEntity.setHtml(document.html());
        log.info("persist document");
        wikiCrawledEntityRepository.save(wikiCrawledEntity);
    }

}
