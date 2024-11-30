package zzk.townshipscheduler.backend.crawling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
@Setter
@Getter
public class TownshipFandomCrawlingProcessFacade {

    private final TownshipDataCrawlingProcessor crawlingProcessor;

    private final TownshipDataParsingProcessor parsingProcessor;

    private final TownshipDataTransferProcessor transferProcessor;

    private final TownshipDataPersistProcessor persistProcessor;

    private final TownshipDataHardcodeHotfixProcessor hardcodeHotfixProcessor;

    private final ExecutorService townshipExecutorService;

    private CrawledResult crawledResult;

    private ParsedResult parsedResult;

    private TransferResult transferResult;

    public Set<String> segmentNameSet() {
        return parsedResult.segmentNameSet();
    }

    public ParsedResultSegment getSegment(String category) {
        return parsedResult.getSegment(category);
    }

    public LinkedHashMap<String, ParsedResultSegment> getZoneDtoMap() {
        return parsedResult.groupByTable();
    }

    public CompletableFuture<Void> process() {
        return crawlingProcessor.process()
                .thenApplyAsync(
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
                .thenAcceptAsync(
                        transferResult -> {
                            setTransferResult(transferResult);
                            this.persistProcessor.process(transferResult);
                        }, townshipExecutorService
                ).thenAcceptAsync(_ -> {
                    this.hardcodeHotfixProcessor.process();
                }, townshipExecutorService);
    }

    public CompletableFuture<byte[]> downloadImage(String url) {
        return crawlingProcessor.downloadImage(url);
    }

}
