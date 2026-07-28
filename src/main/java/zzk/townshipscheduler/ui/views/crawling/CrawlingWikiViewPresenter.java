package zzk.townshipscheduler.ui.views.crawling;

import com.vaadin.flow.component.grid.Grid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import zzk.townshipscheduler.backend.crawling.MhtmlProcessComponent;
import zzk.townshipscheduler.backend.crawling.TownshipFandomCrawlingProcessFacade;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledParsedCoordCellEntityRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlingWikiViewPresenter {

    private final TownshipFandomCrawlingProcessFacade townshipFandomCrawlingProcessFacade;

    private final MhtmlProcessComponent mhtmlProcessComponent;

    private final WikiCrawledEntityRepository wikiCrawledEntityRepository;

    private final WikiCrawledParsedCoordCellEntityRepository wikiCrawledParsedCoordCellEntityRepository;

    private CrawlingWikiView view;

    void setProductsView(CrawlingWikiView crawlingWikiView) {
        this.view = crawlingWikiView;
    }

    CompletableFuture<Void> asyncProcess() {
        return townshipFandomCrawlingProcessFacade.process().whenCompleteAsync(
                (unused, throwable) -> {
                    if (throwable != null) {
                        log.error(throwable.getMessage());
                    }
                    log.info("setup presenter");
                    townshipFandomCrawlingProcessFacade.clean();
                }, townshipFandomCrawlingProcessFacade.getTownshipExecutorService()
        );
    }

    void setupTownshipCoordCellGrid(Grid<WikiCrawledParsedCoordCellEntity> grid) {
        grid.setItems(wikiCrawledParsedCoordCellEntityRepository.findAll());
    }

    boolean boolTownshipCrawled() {
        return wikiCrawledEntityRepository.orderByCreatedDateTimeDescLimit1().isPresent();
    }

    public CompletableFuture<Void> handleUploadSuccess(byte[] data, String fileName) {
        try (var inputStream = new ByteArrayInputStream(data)) {
            this.validateMhtmlHeader(inputStream);

            inputStream.reset();
            var mhtmlResult = this.processUploadedMhtml(inputStream);

            return this.asyncProcessFromUploadedHtml(mhtmlResult);
        } catch (Exception e) {
            log.error("处理上传文件时出错", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public void validateMhtmlHeader(InputStream inputStream)
            throws IOException {
        this.mhtmlProcessComponent.validateMhtmlHeader(inputStream);
    }

    public MhtmlProcessComponent.Result processUploadedMhtml(InputStream inputStream)
            throws IOException {
        return this.mhtmlProcessComponent.processUploadedMhtml(inputStream);
    }

    /**
     * Process uploaded HTML document.
     *
     * @param uploadedDocument The HTML document from user upload
     * @return CompletableFuture with processing result
     */
    CompletableFuture<Void> asyncProcessFromUploadedHtml(MhtmlProcessComponent.Result uploadedDocument) {
        return townshipFandomCrawlingProcessFacade.processFromUploadedHtml(uploadedDocument)
                .whenCompleteAsync(
                        (unused, throwable) -> {
                            if (throwable != null) {
                                log.error("error occur while process preceding：{}", throwable);
                            }
                            log.info("download and finished");
                            townshipFandomCrawlingProcessFacade.clean();
                        }, townshipFandomCrawlingProcessFacade.getTownshipExecutorService()
                );
    }

}
