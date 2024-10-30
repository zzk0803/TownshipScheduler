package zzk.townshipscheduler.backend.crawling;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.persistence.TownshipCrawled;
import zzk.townshipscheduler.backend.persistence.TownshipCrawledRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
class TownshipDataCrawlingProcessor {

    public static final Logger logger = LoggerFactory.getLogger(TownshipDataCrawlingProcessor.class);

    public static final String[] ABANDON_ZONE = {"Gems", "Construction Materials"};

    public static final String TOWNSHIP_FANDOM_GOODS = "https://township.fandom.com/wiki/Goods#All_Goods_List";

    private final Set<RawDataCrawledCell.Img> imageToDownload;

    private final TownshipGameDataInMemoryPool townshipGameDataInMemoryPool;

    private final TownshipCrawledRepository townshipCrawledRepository;

    private final ExecutorService townshipExecutorService;

    private final RetryTemplate retryTemplate;

    private final TransactionTemplate transactionTemplate;

    public TownshipDataCrawlingProcessor(
            TownshipGameDataInMemoryPool townshipGameDataInMemoryPool,
            TownshipCrawledRepository townshipCrawledRepository,
            ExecutorService townshipExecutorService,
            RetryTemplate retryTemplate,
            TransactionTemplate transactionTemplate
    ) {
        this.townshipGameDataInMemoryPool = townshipGameDataInMemoryPool;
        this.townshipCrawledRepository = townshipCrawledRepository;
        this.townshipExecutorService = townshipExecutorService;
        this.retryTemplate = retryTemplate;
        this.transactionTemplate = transactionTemplate;
        this.imageToDownload = new LinkedHashSet<>();
    }

    public CompletableFuture<CrawledResult> process() {
        Document document = loadDocument(false);
        Elements articleTableElements = document.getElementsByClass("article-table");

        for (int i = 0; i < articleTableElements.size(); i++) {
            int tableNum = 1 + i;

            Element currentTable = articleTableElements.get(i);
            String tableZoneString = findTableZoneString(currentTable);
            if (checkAbandonZone(tableZoneString)) {
                continue;
            }

            doTableParse(currentTable, tableNum, tableZoneString);
        }

        fireImageDownloadAsync();

        logger.info("ready to crawled poll do mending and fire image downloading");
        return CompletableFuture.supplyAsync(
                townshipGameDataInMemoryPool::completeAndMend,
                townshipExecutorService
        );
    }

    private static String findTableZoneString(Element currentTable) {
        Optional<String> tableZone = currentTable.parents()
                .stream()
                .filter(element -> element.hasClass("mw-collapsible-content"))
                .findFirst()
                .map(element -> {
                    Element mwHeadlineElement = element.previousElementSibling();
                    return Objects.isNull(mwHeadlineElement)
                            ? ""
                            : mwHeadlineElement.select("span.mw-headline").first().text();
                });
        return tableZone.orElse("");
    }

    private static boolean checkAbandonZone(String tableZoneString) {
        return Arrays.stream(ABANDON_ZONE).anyMatch(tableZoneString::equalsIgnoreCase);
    }

    private Document loadDocument(boolean mandatory) {
        Document document = null;
        if (mandatory) {
            logger.info("mandatory mode,force fetch");
            document = fetchDocument();
            persistDocument(document);
        } else {
            logger.info("get crawled html in db");
            TownshipCrawled townshipCrawled = null;
            Optional<TownshipCrawled> crawledOptional = townshipCrawledRepository.orderByCreatedDateTimeDescLimit1();
            if (crawledOptional.isPresent()) {
                townshipCrawled = crawledOptional.get();
                document = Jsoup.parse(townshipCrawled.getHtml());
            } else {
                logger.warn("not found in db");
                document = fetchDocument();
                persistDocument(document);
            }
        }
        return document;
    }

    private Document fetchDocument() {
        logger.info("fetch random wiki...");
        try {
            return Jsoup.connect(TOWNSHIP_FANDOM_GOODS).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TownshipCrawled persistDocument(Document document) {
        TownshipCrawled townshipCrawled = new TownshipCrawled();
        townshipCrawled.setType(TownshipCrawled.Type.HTML);
        townshipCrawled.setHtml(document.html());
        logger.info("persist document");
        return townshipCrawledRepository.save(townshipCrawled);
    }

    private void doTableParse(
            Element currentTable, int tableNum, String tableZoneString
    ) {
        Elements trElements = currentTable.getElementsByTag("tr");
        int currentRowsSize = trElements.size();

        RawDataCrawledCoord crawledCoordPrototype = new RawDataCrawledCoord();
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
                List<RawDataCrawledCell.Anchor> anchorList = doParseUnitIntoAnchor(currentThOrTd);
                List<RawDataCrawledCell.Img> imgList = doParseUnitIntoImg(currentThOrTd);
                imageToDownload.addAll(imgList);

                int rowSpan = doParseUnitIntoRowSpanInt(currentThOrTd);
                int colSpan = doParseUnitIntoColSpanInt(currentThOrTd);

                crawledCoordPrototype = crawledCoordPrototype.cloneAndNextColumn();

                RawDataCrawledCoord currentCoord = crawledCoordPrototype.clone();
                currentCoord.setTable(tableNum);
                currentCoord.setTableZone(tableZoneString);

                RawDataCrawledCell currentCell = RawDataCrawledCell.builder()
                        .html(html)
                        .text(text)
                        .anchorList(anchorList)
                        .imgList(imgList)
                        .type(currentColumnsSize == 1 ? RawDataCrawledCell.Type.HEAD : RawDataCrawledCell.Type.CELL)
                        .span(new RawDataCrawledCell.CellSpan(rowSpan, colSpan))
                        .build();

                registerSpanFixIfNeed(currentCoord, currentCell, rowSpan, colSpan);

                putIntoMemory(currentCoord, currentCell);
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

    private List<RawDataCrawledCell.Anchor> doParseUnitIntoAnchor(Element currentThOrTd) {
        Elements anchorElements = currentThOrTd.select("a");
        return anchorElements.stream().map(element -> {
            RawDataCrawledCell.Anchor l = new RawDataCrawledCell.Anchor();
            l.setHref(element.attr("href"));
            l.setTitle(element.attr("title"));
            l.setText(element.text());
            return l;
        }).toList();
    }

    private List<RawDataCrawledCell.Img> doParseUnitIntoImg(Element currentThOrTd) {
        Elements imageElements = currentThOrTd.select("img");
        return imageElements.stream().map(element -> {
            RawDataCrawledCell.Img cellImg = new RawDataCrawledCell.Img();
            cellImg.setAlt(element.attr("alt"));
            cellImg.setSrc(element.attr("data-src"));
            return cellImg;
        }).toList();
    }

    private int doParseUnitIntoRowSpanInt(Element currentThOrTd) {
        String rowspan = currentThOrTd.attr("rowspan");
        return rowspan.isBlank() ? 0 : Integer.parseInt(rowspan);
    }

    private int doParseUnitIntoColSpanInt(Element currentThOrTd) {
        String colspan = currentThOrTd.attr("colspan");
        return colspan.isBlank() ? 0 : Integer.parseInt(colspan);
    }

    private void registerSpanFixIfNeed(
            RawDataCrawledCoord currentCoord, RawDataCrawledCell currentCell, int rowSpan, int colSpan
    ) {
        if (currentCell.getType() == RawDataCrawledCell.Type.CELL) {
            registerRowSpanFix(rowSpan, currentCell, currentCoord);
            registerColSpanFix(colSpan, currentCell, currentCoord);
        }
    }

    private void registerRowSpanFix(int rowSpan, RawDataCrawledCell currentCell, RawDataCrawledCoord currentCoord) {
        if (rowSpan > RawDataCrawledCell.CellSpan.NA_EFFECT) {
            RawDataCrawledCell.CellSpan fixedSpan = new RawDataCrawledCell.CellSpan(
                    RawDataCrawledCell.CellSpan.REGULAR,
                    RawDataCrawledCell.CellSpan.REGULAR
            );
            RawDataCrawledCell fixCell = currentCell.clone();
            fixCell.setSpan(fixedSpan);

            int fixSize = rowSpan - 1;
            RawDataCrawledCoord mendedCoord = currentCoord.cloneAndNextRow();
            while (fixSize > 0) {
                hintIntoMemory(mendedCoord, fixCell);
                fixSize -= 1;
                mendedCoord = mendedCoord.cloneAndNextRow();
            }
        }
    }

    private void hintIntoMemory(RawDataCrawledCoord mendedCoord, RawDataCrawledCell fixCell) {
        townshipGameDataInMemoryPool.putForMend(mendedCoord, fixCell);
    }

    private void registerColSpanFix(int colSpan, RawDataCrawledCell currentCell, RawDataCrawledCoord currentCoord) {
        if (colSpan > RawDataCrawledCell.CellSpan.NA_EFFECT) {
            RawDataCrawledCell.CellSpan fixedSpan = new RawDataCrawledCell.CellSpan(
                    RawDataCrawledCell.CellSpan.REGULAR,
                    RawDataCrawledCell.CellSpan.REGULAR
            );
            RawDataCrawledCell fixCell = currentCell.clone();
            fixCell.setSpan(fixedSpan);

            int fixSize = colSpan - 1;
            for (int i = 0; i < fixSize; i++) {
                RawDataCrawledCell fixCellToSave = fixCell.clone();
                fixCellToSave.setText(fixCellToSave.reasonableText() + "[colspan:" + (i + 1) + "]");
                RawDataCrawledCoord mendedCoord = currentCoord.cloneAndNextColumn();
                hintIntoMemory(mendedCoord, fixCellToSave);
            }
        }
    }

    private void putIntoMemory(RawDataCrawledCoord currentCoord, RawDataCrawledCell currentCell) {
        townshipGameDataInMemoryPool.putForSave(currentCoord, currentCell);
    }

    private void fireImageDownloadAsync() {
        CompletableFuture.runAsync(() -> {
            List<CompletableFuture<TownshipCrawled>> completableFutures = imageToDownload.stream()
                    .distinct()
                    .filter(img -> !townshipCrawledRepository.existsByHtml(img.getSrc()))
                    .map(img -> this.downloadImage(img)
                            .thenApplyAsync((bytes) -> {
                                final TownshipCrawled townshipCrawled = new TownshipCrawled();
                                townshipCrawled.setImageBytes(bytes);
                                townshipCrawled.setHtml(img.getSrc());
                                townshipCrawled.setText(img.getAlt());
                                try {
                                    return transactionTemplate.execute(
                                            ts -> townshipCrawledRepository.save(townshipCrawled)
                                    );
                                } catch (TransactionException e) {
                                    e.printStackTrace();
                                    throw new RuntimeException(e);
                                }
                            }, townshipExecutorService)
                    )
                    .toList();
            CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
                    .whenComplete((result, exception) -> {
                        logger.info("all picture download completed...");
                    });
        }, townshipExecutorService);
    }

    CompletableFuture<byte[]> downloadImage(RawDataCrawledCell.Img img) {
        return downloadImage(img.getSrc());
    }

    CompletableFuture<byte[]> downloadImage(String url) {
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
                .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
                ).timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        return CompletableFuture.supplyAsync(() -> {
            byte[] bytes = null;
            try {
                bytes = retryTemplate.execute((RetryCallback<byte[], Throwable>) retryContext -> {
                    try (HttpClient client = HttpClient.newHttpClient()) {
                        HttpResponse<byte[]> httpResponse = client.send(
                                httpRequest, HttpResponse.BodyHandlers.ofByteArray()
                        );
                        return httpResponse.body();
                    } catch (IOException | InterruptedException e) {
                        logger.error(retryContext.getLastThrowable().getMessage());
                        throw new RuntimeException(retryContext.getLastThrowable());
                    }
                });
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return bytes;
        }, townshipExecutorService);

    }

}

/*
                .reduce(
                        CompletableFuture.completedFuture(new CopyOnWriteArrayList<>()),
                        (resultCompletableFuture, processingCompletableFuture) -> {
                            resultCompletableFuture.thenAcceptAsync(townshipCrawled -> {
                                processingCompletableFuture.thenAcceptAsync(
                                        townshipCrawled::add,
                                        townshipExecutorService
                                );
                            }, townshipExecutorService);
                            return resultCompletableFuture;
                        },
                        (resultCompletableFuture1, resultCompletableFuture2) -> {
                            resultCompletableFuture1.thenAcceptAsync(leftResultList -> {
                                resultCompletableFuture2.thenAcceptAsync(
                                        leftResultList::addAll,
                                        townshipExecutorService
                                );
                            }, townshipExecutorService);
                            return resultCompletableFuture1;
                        }
                );
 */
