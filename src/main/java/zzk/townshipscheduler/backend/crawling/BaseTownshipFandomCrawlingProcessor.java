//package zzk.townshipscheduler.backend.crawling;
//
//import org.javatuples.Pair;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.retry.RetryCallback;
//import org.springframework.retry.support.RetryTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.support.TransactionTemplate;
//import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;
//import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledEntityRepository;
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.time.Duration;
//import java.util.*;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutorService;
//import java.util.function.Supplier;
//
//@Component
//public abstract class BaseTownshipFandomCrawlingProcessor {
//
//    public static final Logger logger = LoggerFactory.getLogger(BaseTownshipFandomCrawlingProcessor.class);
//
//    private final Set<CrawledDataCell.Img> imageToDownload;
//
//    private final CrawledDataMemory crawledDataMemory;
//
//    private final WikiCrawledEntityRepository wikiCrawledEntityRepository;
//
//    private final ExecutorService townshipExecutorService;
//
//    private final RetryTemplate retryTemplate;
//
//    private final TransactionTemplate transactionTemplate;
//
//    public BaseTownshipFandomCrawlingProcessor(
//            CrawledDataMemory crawledDataMemory,
//            WikiCrawledEntityRepository wikiCrawledEntityRepository,
//            ExecutorService townshipExecutorService,
//            RetryTemplate retryTemplate,
//            TransactionTemplate transactionTemplate
//    ) {
//        this.crawledDataMemory = crawledDataMemory;
//        this.wikiCrawledEntityRepository = wikiCrawledEntityRepository;
//        this.townshipExecutorService = townshipExecutorService;
//        this.retryTemplate = retryTemplate;
//        this.transactionTemplate = transactionTemplate;
//        this.imageToDownload = new LinkedHashSet<>();
//    }
//
//    public CompletableFuture<CrawledResult> process() {
//        Document document = loadDocument(false);
//        Elements articleTableElements = useFunctionToGetElements(document).get();
//
//        for (int i = 0; i < articleTableElements.size(); i++) {
//            int tableNum = 1 + i;
//
//            Element currentTable = articleTableElements.get(i);
//            String tableZoneString = findTableZoneString(currentTable);
//            //            if (checkAbandonZone(tableZoneString)) {
//            //                continue;
//            //            }
//
//            doTableParse(currentTable, tableNum, tableZoneString);
//        }
//
//        fireImageDownloadAsync();
//
//        logger.info(" do mending and fire image downloading");
//        return CompletableFuture.supplyAsync(crawledDataMemory::completeAndMend, townshipExecutorService);
//    }
//
//    public abstract Supplier<Elements> useFunctionToGetElements(Document document);
//
//    protected  String findTableZoneString(Element currentTable) {
//        return currentTable.parents()
//                .stream()
//                .filter(element -> element.hasClass("mw-collapsible-content"))
//                .findFirst()
//                .map(element -> {
//                    Element mwHeadlineElement = element.previousElementSibling();
//                    return Objects.isNull(mwHeadlineElement)
//                            ? ""
//                            : mwHeadlineElement.select("span.mw-headline").first().text();
//                }).orElse("");
//    }
//
//    //    private static boolean checkAbandonZone(String tableZoneString) {
//    //        return Arrays.stream(ABANDON_ZONE).anyMatch(tableZoneString::equalsIgnoreCase);
//    //    }
//
//    private Document loadDocument(boolean mandatory) {
//        Document document = null;
//        if (mandatory) {
//            logger.info("mandatory mode,force fetch");
//            document = fetchDocument();
//            persistDocument(document);
//        } else {
//            logger.info("get crawled html in db");
//            WikiCrawledEntity wikiCrawledEntity = null;
//            Optional<WikiCrawledEntity> crawledOptional = wikiCrawledEntityRepository.orderByCreatedDateTimeDescLimit1();
//            if (crawledOptional.isPresent()) {
//                wikiCrawledEntity = crawledOptional.get();
//                document = Jsoup.parse(wikiCrawledEntity.getHtml());
//            } else {
//                logger.warn("not found in db");
//                document = fetchDocument();
//                persistDocument(document);
//            }
//        }
//        return document;
//    }
//
//    private Document fetchDocument() {
//        logger.info("fetch random wiki...");
//        try {
//            String url = useUrlStringAsFetchSource();
//            if (url == null || url.isEmpty()) {
//                throw new IllegalArgumentException();
//            }
//            return Jsoup.connect(url).get();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public abstract String useUrlStringAsFetchSource();
//
//    private void persistDocument(Document document) {
//        WikiCrawledEntity wikiCrawledEntity = new WikiCrawledEntity();
//        wikiCrawledEntity.setType(WikiCrawledEntity.Type.HTML);
//        wikiCrawledEntity.setHtml(document.html());
//        logger.info("persist document");
//        wikiCrawledEntityRepository.save(wikiCrawledEntity);
//    }
//
//    private void doTableParse(Element currentTable, int tableNum, String tableZoneString) {
//        Elements trElements = currentTable.getElementsByTag("tr");
//        int currentRowsSize = trElements.size();
//
//        CrawledDataCoordinate crawledCoordPrototype = new CrawledDataCoordinate();
//        //row iteration
//        for (int currentRowNum = 0; currentRowNum < currentRowsSize; currentRowNum++) {
//            Element currentRow = trElements.get(currentRowNum);
//            Elements trChildrenElements = currentRow.children();
//            int currentColumnsSize = trChildrenElements.size();
//
//            crawledCoordPrototype = crawledCoordPrototype.cloneAndNextRow();
//            //column iteration
//            for (int currentColumnNum = 0; currentColumnNum < currentColumnsSize; currentColumnNum++) {
//                Element currentThOrTd = trChildrenElements.get(currentColumnNum);
//
//                String html = doParseUnitIntoHtml(currentThOrTd);
//                String text = doParseUnitIntoText(currentThOrTd);
//                List<CrawledDataCell.Anchor> anchorList = doParseUnitIntoAnchor(currentThOrTd);
//                List<CrawledDataCell.Img> imgList = doParseUnitIntoImg(currentThOrTd);
//                imageToDownload.addAll(imgList);
//
//                int rowSpan = doParseUnitIntoRowSpanInt(currentThOrTd);
//                int colSpan = doParseUnitIntoColSpanInt(currentThOrTd);
//
//                crawledCoordPrototype = crawledCoordPrototype.cloneAndNextColumn();
//
//                CrawledDataCoordinate currentCoord = crawledCoordPrototype.clone();
//                currentCoord.setTable(tableNum);
//                currentCoord.setTableZone(tableZoneString);
//
//                CrawledDataCell currentCell = CrawledDataCell.builder()
//                        .html(html)
//                        .text(text)
//                        .anchorList(anchorList)
//                        .imgList(imgList)
//                        .type(currentColumnsSize == 1 ? CrawledDataCell.Type.HEAD : CrawledDataCell.Type.CELL)
//                        .span(new CrawledDataCell.CellSpan(rowSpan, colSpan))
//                        .build();
//
//                registerSpanFixIfNeed(currentCoord, currentCell, rowSpan, colSpan);
//
//                putIntoMemory(currentCoord, currentCell);
//            }
//            crawledCoordPrototype = crawledCoordPrototype.cloneAndResetColumn();
//        }
//    }
//
//    private String doParseUnitIntoHtml(Element currentThOrTd) {
//        return currentThOrTd.html();
//    }
//
//    private String doParseUnitIntoText(Element currentThOrTd) {
//        return currentThOrTd.text();
//    }
//
//    private List<CrawledDataCell.Anchor> doParseUnitIntoAnchor(Element currentThOrTd) {
//        Elements anchorElements = currentThOrTd.select("a");
//        return anchorElements.stream().map(element -> {
//            CrawledDataCell.Anchor l = new CrawledDataCell.Anchor();
//            l.setHref(element.attr("href"));
//            l.setTitle(element.attr("title"));
//            l.setText(element.text());
//            return l;
//        }).toList();
//    }
//
//    private List<CrawledDataCell.Img> doParseUnitIntoImg(Element currentThOrTd) {
//        Elements imageElements = currentThOrTd.select("img");
//        return imageElements.stream().map(element -> {
//            CrawledDataCell.Img cellImg = new CrawledDataCell.Img();
//            cellImg.setAlt(element.attr("alt"));
//            cellImg.setSrc(element.attr("data-src"));
//            return cellImg;
//        }).toList();
//    }
//
//    private int doParseUnitIntoRowSpanInt(Element currentThOrTd) {
//        String rowspan = currentThOrTd.attr("rowspan");
//        return rowspan.isBlank() ? 0 : Integer.parseInt(rowspan);
//    }
//
//    private int doParseUnitIntoColSpanInt(Element currentThOrTd) {
//        String colspan = currentThOrTd.attr("colspan");
//        return colspan.isBlank() ? 0 : Integer.parseInt(colspan);
//    }
//
//    private void registerSpanFixIfNeed(
//            CrawledDataCoordinate currentCoord,
//            CrawledDataCell currentCell,
//            int rowSpan,
//            int colSpan
//    ) {
//        if (currentCell.getType() == CrawledDataCell.Type.CELL) {
//            registerRowSpanFix(rowSpan, currentCell, currentCoord);
//            registerColSpanFix(colSpan, currentCell, currentCoord);
//        }
//    }
//
//    private void registerRowSpanFix(int rowSpan, CrawledDataCell currentCell, CrawledDataCoordinate currentCoord) {
//        if (rowSpan > CrawledDataCell.CellSpan.NA_EFFECT) {
//            CrawledDataCell.CellSpan fixedSpan = new CrawledDataCell.CellSpan(
//                    CrawledDataCell.CellSpan.REGULAR,
//                    CrawledDataCell.CellSpan.REGULAR
//            );
//            CrawledDataCell cellFixed = currentCell.clone();
//            cellFixed.setSpan(fixedSpan);
//
//            int fixSize = rowSpan - 1;
//            CrawledDataCoordinate mendedCoord = currentCoord.cloneAndNextRow();
//            while (fixSize > 0) {
//                hintIntoMemory(mendedCoord, cellFixed);
//                fixSize -= 1;
//                mendedCoord = mendedCoord.cloneAndNextRow();
//            }
//        }
//    }
//
//    private void hintIntoMemory(CrawledDataCoordinate mendedCoord, CrawledDataCell fixCell) {
//        crawledDataMemory.putForMend(mendedCoord, fixCell);
//    }
//
//    private void registerColSpanFix(int colSpan, CrawledDataCell currentCell, CrawledDataCoordinate currentCoord) {
//        if (colSpan > CrawledDataCell.CellSpan.NA_EFFECT) {
//            CrawledDataCell.CellSpan fixedSpan = new CrawledDataCell.CellSpan(
//                    CrawledDataCell.CellSpan.REGULAR,
//                    CrawledDataCell.CellSpan.REGULAR
//            );
//            CrawledDataCell cellFixed = currentCell.clone();
//            cellFixed.setSpan(fixedSpan);
//
//            int fixSize = colSpan - 1;
//            for (int i = 0; i < fixSize; i++) {
//                CrawledDataCell fixCellToSave = cellFixed.clone();
//                fixCellToSave.setText(fixCellToSave.reasonableText() + "[colspan:" + (i + 1) + "]");
//                CrawledDataCoordinate mendedCoord = currentCoord.cloneAndNextColumn();
//                hintIntoMemory(mendedCoord, fixCellToSave);
//            }
//        }
//    }
//
//    private void putIntoMemory(CrawledDataCoordinate currentCoord, CrawledDataCell currentCell) {
//        crawledDataMemory.putForSave(currentCoord, currentCell);
//    }
//
//    private void fireImageDownloadAsync() {
//        CompletableFuture.runAsync(
//                () -> {
//                    List<CompletableFuture<WikiCrawledEntity>> completableFutures = imageToDownload.stream()
//                            .distinct()
//                            .filter(img -> !wikiCrawledEntityRepository.existsByHtml(img.getSrc()))
//                            .map(rawData_Img -> {
//                                WikiCrawledEntity wikiCrawledEntity = new WikiCrawledEntity();
//                                wikiCrawledEntity.setHtml(rawData_Img.getSrc());
//                                wikiCrawledEntity.setText(rawData_Img.getAlt());
//
//                                return new Pair<>(
//                                        rawData_Img,
//                                        transactionTemplate.execute(
//                                                _ -> wikiCrawledEntityRepository.save(wikiCrawledEntity)
//                                        )
//                                );
//                            })
//                            .map(Pair_RawDataImg_CrawledEntity -> {
//                                CrawledDataCell.Img img = Pair_RawDataImg_CrawledEntity.getValue0();
//                                WikiCrawledEntity wikiCrawledEntity = Pair_RawDataImg_CrawledEntity.getValue1();
//                                CompletableFuture<byte[]> completableFuture = this.downloadImage(img);
//                                return completableFuture.thenApplyAsync(
//                                        (bytes) -> {
//                                            wikiCrawledEntity.setImageBytes(bytes);
//                                            return transactionTemplate.execute(
//                                                    _ -> wikiCrawledEntityRepository.save(wikiCrawledEntity)
//                                            );
//                                        }, townshipExecutorService
//                                );
//                            })
//                            .toList();
//                    CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
//                            .whenComplete((result, exception) -> {
//                                logger.info("all picture download completed...");
//                            });
//                }, townshipExecutorService
//        );
//    }
//
//    CompletableFuture<byte[]> downloadImage(CrawledDataCell.Img img) {
//        return downloadImage(img.getSrc());
//    }
//
//    CompletableFuture<byte[]> downloadImage(String url) {
//        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).header(
//                "User-Agent",
//                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
//        ).timeout(Duration.ofSeconds(20)).GET().build();
//
//        return CompletableFuture.supplyAsync(
//                () -> {
//                    byte[] bytes = null;
//                    try {
//                        bytes = retryTemplate.execute((RetryCallback<byte[], Throwable>) retryContext -> {
//                            try (HttpClient client = HttpClient.newHttpClient()) {
//                                HttpResponse<byte[]> httpResponse = client.send(
//                                        httpRequest,
//                                        HttpResponse.BodyHandlers.ofByteArray()
//                                );
//                                return httpResponse.body();
//                            } catch (IOException | InterruptedException e) {
//                                logger.error(retryContext.getLastThrowable().getMessage());
//                                throw new RuntimeException(retryContext.getLastThrowable());
//                            }
//                        });
//                    } catch (Throwable e) {
//                        throw new RuntimeException(e);
//                    }
//                    return bytes;
//                }, townshipExecutorService
//        );
//
//    }
//
//
//}
