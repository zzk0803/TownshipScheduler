package zzk.townshipscheduler.backend.crawling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
@Setter
@Getter
public class TownshipFandomCrawlingProcessFacade {

    private final TownshipDataCrawlingProcessor crawlingProcessor;

    private final TownshipOfflineDataCrawlingProcessor offlineProcessor;

    private final TownshipDataParsingProcessor parsingProcessor;

    private final TownshipDataMappingProcessor transferProcessor;

    private final TownshipDataPersistProcessor persistProcessor;

    private final TownshipDataHierarchyBuildingProcessor hierarchyBuildingProcessor;

    private final TownshipDataHardcodeHotfixProcessor hardcodeHotfixProcessor;

    private final ExecutorService townshipExecutorService;

    private CrawledResult crawledResult;

    private ParsedResult parsedResult;

    private TransferResult transferResult;

    private PersistResult persistResult;

    private HierarchyResult hierarchyResult;

    public CompletableFuture<Void> process() {
        CompletableFuture<CrawledResult> crawledResultCompletableFuture = crawlingProcessor.process();
        return afterCrawlingProcess(crawledResultCompletableFuture);
    }

    public CompletableFuture<Void> afterCrawlingProcess(CompletableFuture<CrawledResult> crawledResultCompletableFuture) {
        return crawledResultCompletableFuture.thenApplyAsync(
                        crawledResult -> {
                            setCrawledResult(crawledResult);
                            persistProcessor.process(crawledResult);
                            return parsingProcessor.process(crawledResult);
                        }, townshipExecutorService
                )
                .thenApplyAsync(
                        parsedResult -> {
                            setParsedResult(parsedResult);
                            return this.transferProcessor.process(parsedResult);
                        }, townshipExecutorService
                )
                .thenApplyAsync(
                        transferResult -> {
                            setTransferResult(transferResult);
                            return this.persistProcessor.process(transferResult);
                        }, townshipExecutorService
                )
                .thenApplyAsync(
                        persistResult -> {
                            setPersistResult(persistResult);
                            return this.hierarchyBuildingProcessor.process(persistResult);
                        }, townshipExecutorService
                )
                .thenAcceptAsync(
                        _ -> {
                            this.hardcodeHotfixProcessor.process();
                        }, townshipExecutorService
                );
    }

    public CompletableFuture<Void> processFromOfflineMhtmlAsTxt() {
        CompletableFuture<CrawledResult> crawledResultCompletableFuture = offlineProcessor.processFromOfflineMhtmlAsTxt();
        return afterCrawlingProcess(crawledResultCompletableFuture);
    }

    public CompletableFuture<Void> processFromOfflineMhtmlAsTxt(MhtmlProcessComponent.Result mhtmlResult) {
        CompletableFuture<CrawledResult> crawledResultCompletableFuture = offlineProcessor.processFromOfflineMhtmlAsTxt(mhtmlResult);
        return afterCrawlingProcess(crawledResultCompletableFuture);
    }

    public void clean() {
        crawledResult = null;
        parsedResult = null;
        transferResult = null;
        persistResult = null;
        hierarchyResult = null;
    }

}
