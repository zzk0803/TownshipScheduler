package zzk.townshipscheduler.backend.crawling;

import jakarta.annotation.PreDestroy;
import org.javatuples.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.dao.WikiCrawledEntityRepository;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Component
class TownshipDataCrawlingProcessor {

    public static final Logger logger = LoggerFactory.getLogger(TownshipDataCrawlingProcessor.class);

    public static final String[] ABANDON_ZONE = {"Gems", "Construction Materials"};

    public static final String TOWNSHIP_FANDOM_GOODS = "https://township.fandom.com/wiki/Goods#All_Goods_List";

    private final Set<CrawledDataCell.Img> imageToDownload;

    private final CrawledDataMemory crawledDataMemory;

    private final WikiCrawledEntityRepository wikiCrawledEntityRepository;

    private final ExecutorService townshipExecutorService;

    private final RetryTemplate retryTemplate;

    private final TransactionTemplate transactionTemplate;

    private final HttpClient httpClient;

    public TownshipDataCrawlingProcessor(
            CrawledDataMemory crawledDataMemory,
            WikiCrawledEntityRepository wikiCrawledEntityRepository,
            ExecutorService townshipExecutorService,
            RetryTemplate retryTemplate,
            TransactionTemplate transactionTemplate
    ) {
        this.crawledDataMemory = crawledDataMemory;
        this.wikiCrawledEntityRepository = wikiCrawledEntityRepository;
        this.townshipExecutorService = townshipExecutorService;
        this.retryTemplate = retryTemplate;
        this.transactionTemplate = transactionTemplate;
        this.imageToDownload = new LinkedHashSet<>();
        this.httpClient = HttpClient.newHttpClient();
    }

    @PreDestroy
    public void close() {
        this.httpClient.close();
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

        logger.info(" do mending and fire image downloading");
        CompletableFuture.supplyAsync(
                this::fireImageDownloadAsync,
                townshipExecutorService
        );
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
                            : mwHeadlineElement.select("span.mw-headline").first().text();
                });
        return tableZone.orElse("");
    }

    private boolean checkAbandonZone(String tableZoneString) {
        return Arrays.stream(ABANDON_ZONE).anyMatch(tableZoneString::equalsIgnoreCase);
    }

    private Document loadDocument(boolean mandatory) {
        Document document = null;
        if (mandatory) {
            logger.info("mandatory mode,force fetch");
            try {
                document = fetchDocument();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            persistDocument(document);
        } else {
            logger.info("get crawled html in db");
            WikiCrawledEntity wikiCrawledEntity = null;
            Optional<WikiCrawledEntity> crawledOptional = wikiCrawledEntityRepository.orderByCreatedDateTimeDescLimit1();
            if (crawledOptional.isPresent()) {
                wikiCrawledEntity = crawledOptional.get();
                document = Jsoup.parse(wikiCrawledEntity.getHtml());
            } else {
                logger.warn("not found in db");
                try {
                    document = fetchDocument();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                persistDocument(document);
            }
        }
        return document;
    }

    private Document fetchDocument() throws Throwable {
        return retryTemplate.execute(
                (RetryCallback<Document, Throwable>) retryContext -> {
                    logger.info(
                            "try to establish connection to fandom wiki ..retry X {}",
                            retryContext.getRetryCount()
                    );
                    return Jsoup.connect(TOWNSHIP_FANDOM_GOODS).get();
                }
        );
    }

    private void persistDocument(Document document) {
        WikiCrawledEntity wikiCrawledEntity = new WikiCrawledEntity();
        wikiCrawledEntity.setType(WikiCrawledEntity.Type.HTML);
        wikiCrawledEntity.setHtml(document.html());
        logger.info("persist document");
        wikiCrawledEntityRepository.save(wikiCrawledEntity);
    }

    private void doTableParse(Element currentTable, int tableNum, String tableZoneString) {
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
                        .type(currentColumnsSize == 1 ? CrawledDataCell.Type.HEAD : CrawledDataCell.Type.CELL)
                        .span(new CrawledDataCell.CellSpan(rowSpan, colSpan))
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

    private List<CrawledDataCell.Anchor> doParseUnitIntoAnchor(Element currentThOrTd) {
        Elements anchorElements = currentThOrTd.select("a");
        return anchorElements.stream().map(element -> {
            CrawledDataCell.Anchor l = new CrawledDataCell.Anchor();
            l.setHref(element.attr("href"));
            l.setTitle(element.attr("title"));
            l.setText(element.text());
            return l;
        }).toList();
    }

    private List<CrawledDataCell.Img> doParseUnitIntoImg(Element currentThOrTd) {
        Elements imageElements = currentThOrTd.select("img");
        return imageElements.stream().map(element -> {
            CrawledDataCell.Img cellImg = new CrawledDataCell.Img();
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
            CrawledDataCoordinate currentCoordinate,
            CrawledDataCell currentCell,
            int rowSpan,
            int colSpan
    ) {
        if (currentCell.getType() == CrawledDataCell.Type.CELL) {
            registerRowSpanFix(rowSpan, currentCell, currentCoordinate);
            registerColSpanFix(colSpan, currentCell, currentCoordinate);
        }
    }

    private void registerRowSpanFix(int rowSpan, CrawledDataCell currentCell, CrawledDataCoordinate currentCoordinate) {
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
                hintIntoMemory(mendedCoord, cellFixed);
                fixSize -= 1;
                mendedCoord = mendedCoord.cloneAndNextRow();
            }
        }
    }

    private void hintIntoMemory(CrawledDataCoordinate currentCoordinate, CrawledDataCell fixCell) {
        crawledDataMemory.putForMend(currentCoordinate, fixCell);
    }

    private void registerColSpanFix(int colSpan, CrawledDataCell currentCell, CrawledDataCoordinate currentCoordinate) {
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
                hintIntoMemory(mendedCoord, fixCellToSave);
            }
        }
    }

    private void putIntoMemory(CrawledDataCoordinate currentCoordinate, CrawledDataCell currentCell) {
        crawledDataMemory.putForSave(currentCoordinate, currentCell);
    }

    private CompletableFuture<Void> fireImageDownloadAsync() {
        logger.info("image downloader start...");

        List<Pair<CrawledDataCell.Img, WikiCrawledEntity>> pairList = imageToDownload.stream()
                .distinct()
                .filter(img -> !wikiCrawledEntityRepository.existsByHtml(img.getSrc()))
                .map(img -> {
                    WikiCrawledEntity wikiCrawledEntity = new WikiCrawledEntity();
                    wikiCrawledEntity.setHtml(img.getSrc());
                    wikiCrawledEntity.setText(img.getAlt());
                    wikiCrawledEntity = wikiCrawledEntityRepository.save(wikiCrawledEntity);
                    return new Pair<>(img, wikiCrawledEntity);
                })
                .toList();

        List<CompletableFuture<WikiCrawledEntity>> downloadFutures
                = pairList.stream().
                map(
                        pair -> CompletableFuture.supplyAsync(
                                        () -> {
                                            pair.getValue1().setImageBytes(
                                                    this.downloadImage(pair.getValue0().getSrc())
                                            );
                                            return wikiCrawledEntityRepository.save(pair.getValue1());
                                        }, townshipExecutorService
                                )
                                .orTimeout(30, TimeUnit.SECONDS)
                                .thenApplyAsync(
                                        crawledEntity -> {
                                            logger.info("{} image download completed", pair.getValue0().getSrc());
                                            return crawledEntity;
                                        }, townshipExecutorService
                                )
                                .exceptionallyAsync(
                                        throwable -> {
                                            logger.warn("Failed to download image: {}", pair.getValue0().getSrc());
                                            return null;
                                        }, townshipExecutorService
                                )
                )
                .toList();


        return CompletableFuture.allOf(downloadFutures.toArray(CompletableFuture[]::new))
                .thenRunAsync(
                        () -> {
                            logger.info("All image downloads completed. Count: {}", downloadFutures.size());
                            imageToDownload.clear();
                        }, townshipExecutorService
                );
    }

    byte[] downloadImage(String url) {
        logger.info("start download image {}", url);
        return this.retryTemplate.execute(
                retryContext -> {
                    try {
                        HttpResponse<byte[]> httpResponse = httpClient.send(
                                HttpRequest.newBuilder(URI.create(url))
                                        .header(
                                                "User-Agent",
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
                                        ).timeout(Duration.ofSeconds(5))
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.ofByteArray()
                        );
                        return httpResponse.body();
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
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

/*
@Service
public class ArticleService {

    // 关键：记录“哪些 article 正在爬图”，避免重复触发
    private final Map<Long, CompletableFuture<String>> pendingCrawls = new ConcurrentHashMap<>();

    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor(); // Java 21+

    // 主方法：获取带图片的文章（如果没图，就去爬）
    public CompletableFuture<Article> getArticleWithImage(Long articleId) {
        // 1. 先查数据库
        Article article = articleRepository.findById(articleId).orElse(null);
        if (article != null && article.getImageUrl() != null) {
            // 已有图，直接返回
            return CompletableFuture.completedFuture(article);
        }

        // 2. 没图？检查是否已有爬取任务在进行
        return pendingCrawls.computeIfAbsent(articleId, id -> {
            // 3. 启动爬取任务（仅当没有 pending 任务时）
            return crawlImageAsync(id)
                .whenComplete((url, ex) -> {
                    // 4. 任务完成后，从 pending 移除（无论成功失败）
                    pendingCrawls.remove(id);
                    if (ex == null) {
                        // 5. 成功则更新数据库
                        articleRepository.updateImageUrl(id, url);
                    }
                });
        })
        .thenApply(imageUrl -> {
            // 6. 构造最终结果（这里假设 article 文本已存在）
            if (article == null) {
                article = new Article(); // 或抛异常
                article.setId(articleId);
            }
            article.setImageUrl(imageUrl);
            return article;
        });
    }

    // 模拟爬图（返回 CF 图片 URL）
    private CompletableFuture<String> crawlImageAsync(Long articleId) {
        return CompletableFuture.supplyAsync(() -> {
            // 模拟耗时爬取
            try { Thread.sleep(2000); } catch (InterruptedException e) { }
            return "https://cf.example.com/image-" + articleId + ".jpg";
        }, ioExecutor);
    }
}
 */
