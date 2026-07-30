package zzk.townshipscheduler.backend.crawling;

import io.arxila.javatuples.Pair;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledParsedCoordCellEntityRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Gatherers;

@Slf4j
@Component
class TownshipDataCrawlingProcessor
        extends AbstractTownshipDataCrawlingProcessor {

    public static final String[] ABANDON_ZONE = {"Gems", "Construction Materials"};

    public static final String TOWNSHIP_FANDOM_GOODS = "https://township.fandom.com/wiki/Goods#All_Goods_List";

    public static final URI TOWNSHIP_FANDOM_GOODS_URI = URI.create(TOWNSHIP_FANDOM_GOODS);

    private final RetryTemplate retryTemplate;

    private final HttpClient httpClient;

    public TownshipDataCrawlingProcessor(
            CrawledDataMemory crawledDataMemory,
            WikiCrawledEntityRepository wikiCrawledEntityRepository,
            WikiCrawledParsedCoordCellEntityRepository wikiCrawledParsedCoordCellEntityRepository,
            ExecutorService townshipExecutorService,
            RetryTemplate retryTemplate,
            TransactionTemplate transactionTemplate,
            HttpClient httpClient
    ) {
        super(crawledDataMemory, wikiCrawledEntityRepository, wikiCrawledParsedCoordCellEntityRepository, townshipExecutorService, transactionTemplate);
        this.retryTemplate = retryTemplate;
        this.httpClient = httpClient;
    }

    @PreDestroy
    public void close() {
        this.httpClient.close();
    }

    private Document fetchDocument(URI documentUri)
            throws Throwable {
        return retryTemplate.execute(
                () -> {
                    log.info(
                            "try to establish connection to fandom wiki {} ..",
                            documentUri
                    );
//                    Connection connect = Jsoup.connect(TOWNSHIP_FANDOM_GOODS);
                    try {
                        return Jsoup.parse(
                                documentUri.toURL(),
                                10000
                        );
                    } catch (IOException e) {
                        log.error(e.toString());
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public CompletableFuture<CrawledResult> process() {
        Document document = loadDocument(true);
        Elements articleTableElements = document.getElementsByClass("article-table");
        CompletableFuture<CrawledResult> completableFuture = doProcessTables(articleTableElements);

        log.info("do mending and fire image downloading");
        CompletableFuture.supplyAsync(
                this::fireImageDownloadAsync,
                townshipExecutorService
        );
        return completableFuture;
    }

    private Document loadDocument(boolean mandatory) {
        Document document = null;
        if (mandatory) {
            log.info("clear fetched document");
            wikiCrawledEntityRepository.deleteAll();
            wikiCrawledParsedCoordCellEntityRepository.deleteAll();

            log.info("mandatory mode,force fetch");
            try {
                document = fetchDocument();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            persistDocument(document);
        } else {
            log.info("get crawled html in db");
            WikiCrawledEntity wikiCrawledEntity = null;
            Optional<WikiCrawledEntity> crawledOptional = wikiCrawledEntityRepository.orderByCreatedDateTimeDescLimit1();
            if (crawledOptional.isPresent()) {
                wikiCrawledEntity = crawledOptional.get();
                document = Jsoup.parse(wikiCrawledEntity.getHtml());
            } else {
                log.warn("not found in db");
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

    private Document fetchDocument()
            throws Throwable {
        return retryTemplate.execute(
                () -> {
                    log.info(
                            "try to establish connection to fandom wiki .."
                    );
//                    Connection connect = Jsoup.connect(TOWNSHIP_FANDOM_GOODS);
                    return Jsoup.parse(
                            TOWNSHIP_FANDOM_GOODS_URI.toURL(),
                            10000
                    );
                }
        );
    }

    private CompletableFuture<Void> fireImageDownloadAsync() {
        log.info("image downloader start...");

        List<Pair<CrawledDataCell.Img, WikiCrawledEntity>> pairList = imageToDownload.stream()
                .distinct()
                .filter(img -> !wikiCrawledEntityRepository.existsByHtml(img.getSrc()))
                .map(img -> {
                    WikiCrawledEntity wikiCrawledEntity = new WikiCrawledEntity();
                    wikiCrawledEntity.setHtml(img.getSrc());
                    wikiCrawledEntity.setText(img.getAlt());
                    wikiCrawledEntity = wikiCrawledEntityRepository.save(wikiCrawledEntity);
                    return new Pair<>(
                            img,
                            wikiCrawledEntity
                    );
                })
                .toList();

        List<CompletableFuture<WikiCrawledEntity>> downloadFutures
                = pairList.stream()
                .gather(
                        Gatherers.mapConcurrent(
                                Runtime.getRuntime()
                                        .availableProcessors(),
                                pair -> CompletableFuture.supplyAsync(
                                                () -> {
                                                    pair.value1()
                                                            .setImageBytes(
                                                                    this.downloadImage(
                                                                            pair.value0().getSrc()
                                                                    )
                                                            );
                                                    return wikiCrawledEntityRepository.save(pair.value1());
                                                },
                                                townshipExecutorService
                                        )
                                        .orTimeout(
                                                30,
                                                TimeUnit.SECONDS
                                        )
                                        .thenApplyAsync(
                                                crawledEntity -> {
                                                    log.info(
                                                            "{} image download completed",
                                                            pair.value0()
                                                                    .getSrc()
                                                    );
                                                    return crawledEntity;
                                                },
                                                townshipExecutorService
                                        )
                                        .exceptionallyAsync(
                                                _ -> {
                                                    log.warn(
                                                            "Failed to download image: {}",
                                                            pair.value0()
                                                                    .getSrc()
                                                    );
                                                    return null;
                                                },
                                                townshipExecutorService
                                        )
                        )
                )
                .toList();


        return CompletableFuture.allOf(downloadFutures.toArray(CompletableFuture[]::new))
                .thenRunAsync(
                        () -> {
                            log.info(
                                    "All image downloads completed. Count: {}",
                                    downloadFutures.size()
                            );
                            imageToDownload.clear();
                        },
                        townshipExecutorService
                );
    }

    byte[] downloadImage(String url) {
        log.info(
                "start download image {}",
                url
        );
        try {
            return this.retryTemplate.execute(
                    () -> {
                        try {
                            HttpResponse<byte[]> httpResponse = httpClient.send(
                                    HttpRequest.newBuilder(URI.create(url))
                                            .header(
                                                    "User-Agent",
                                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537" +
                                                    ".36 Edg/143.0.0.0"
                                            )
                                            .timeout(Duration.ofSeconds(5))
                                            .GET()
                                            .build(),
                                    HttpResponse.BodyHandlers.ofByteArray()
                            );
                            return httpResponse.body();
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                    }
            );
        } catch (RetryException e) {
            return null;
        }
    }

}
