package zzk.townshipscheduler.backend.crawling;

import io.arxila.javatuples.Pair;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntityRepository;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntityRepository;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TownshipOfflineDataCrawlingProcessor
        extends AbstractTownshipDataCrawlingProcessor {

    private final MhtmlImageExtractor mhtmlImageExtractor;

    private final MhtmlProcessComponent mhtmlProcessComponent;

    @Value("classpath:Goods _ Township Wiki _ Fandom.txt")
    private File townshipOfflineWikiFile;

    public TownshipOfflineDataCrawlingProcessor(
            CrawledDataMemory crawledDataMemory,
            WikiCrawledEntityRepository wikiCrawledEntityRepository,
            WikiCrawledParsedCoordCellEntityRepository wikiCrawledParsedCoordCellEntityRepository,
            MhtmlImageExtractor mhtmlImageExtractor,
            MhtmlProcessComponent mhtmlProcessComponent,
            ExecutorService townshipExecutorService,
            RetryTemplate retryTemplate,
            TransactionTemplate transactionTemplate
    ) {
        super(crawledDataMemory, wikiCrawledEntityRepository, wikiCrawledParsedCoordCellEntityRepository, townshipExecutorService, transactionTemplate);
        this.mhtmlImageExtractor = mhtmlImageExtractor;
        this.mhtmlProcessComponent = mhtmlProcessComponent;
    }

    public CompletableFuture<CrawledResult> processFromOfflineMhtmlAsTxt(MhtmlProcessComponent.Result mhtmlResult) {
        Document document = loadDocument(mhtmlResult.document());
        Elements articleTableElements = document.getElementsByClass("article-table");
        CompletableFuture<CrawledResult> completableFuture = doProcessTables(articleTableElements);

        log.info("do mending and fire image downloading");
        CompletableFuture.supplyAsync(
                () -> this.fireImageDownloadFromMhtmlAsync(mhtmlResult.multipart()),
                townshipExecutorService
        );
        return completableFuture;
    }

    private Document loadDocument(Document providedDocument) {
        log.info("clear fetched document");
        wikiCrawledEntityRepository.deleteAll();
        wikiCrawledParsedCoordCellEntityRepository.deleteAll();

        persistDocument(providedDocument);

        return providedDocument;
    }

    private CompletableFuture<List<WikiCrawledEntity>> fireImageDownloadFromMhtmlAsync(MimeMultipart multipart) {
        log.info("image downloader start...");
        try {
            List<MhtmlImageExtractor.ImageData> imageDataList = mhtmlImageExtractor.processMultipart(multipart);
            var urlBytesMap = imageDataList.stream()
                    .collect(Collectors.toMap(
                            MhtmlImageExtractor.ImageData::getContentLocation,
                            MhtmlImageExtractor.ImageData::getData
                    ));
            log.info("mhtml parsed {}", urlBytesMap.keySet());

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

            List<WikiCrawledEntity> downloadFutures
                    = pairList.stream()
                    .map(pair -> {
                        String url = pair.value0().getSrc();
                        pair.value1()
                                .setImageBytes(
                                        urlBytesMap.get(url)
                                );
                        return wikiCrawledEntityRepository.save(pair.value1());
                    })
                    .toList();


            return CompletableFuture.completedFuture(downloadFutures);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

    }

    public CompletableFuture<CrawledResult> processFromOfflineMhtmlAsTxt() {
        MhtmlProcessComponent.Result result = this.mhtmlProcessComponent.processMhtmlFile(townshipOfflineWikiFile);
        return this.processFromOfflineMhtmlAsTxt(result);
    }

}
