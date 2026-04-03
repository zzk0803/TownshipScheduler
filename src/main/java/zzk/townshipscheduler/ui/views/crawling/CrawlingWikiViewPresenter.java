package zzk.townshipscheduler.ui.views.crawling;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zzk.townshipscheduler.backend.crawling.MhtmlProcessComponent;
import zzk.townshipscheduler.backend.crawling.TownshipFandomCrawlingProcessFacade;
import zzk.townshipscheduler.backend.dao.WikiCrawledEntityRepository;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;
import zzk.townshipscheduler.backend.dao.WikiCrawledParsedCoordCellEntityRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlingWikiViewPresenter {

    public static final Logger logger = LoggerFactory.getLogger(CrawlingWikiViewPresenter.class);

    private final TownshipFandomCrawlingProcessFacade townshipFandomCrawlingProcessFacade;

    private final MhtmlProcessComponent mhtmlProcessComponent;

    private final WikiCrawledEntityRepository wikiCrawledEntityRepository;

    private final WikiCrawledParsedCoordCellEntityRepository wikiCrawledParsedCoordCellEntityRepository;

    private CrawlingWikiView view;

    void setProductsView(CrawlingWikiView crawlingWikiView) {
        this.view = crawlingWikiView;
    }

    CompletableFuture<Void> asyncProcess() {
        return townshipFandomCrawlingProcessFacade.process()
                .whenCompleteAsync((unused, throwable) -> {
                    if (throwable != null) {
                        logger.error(throwable.getMessage());
                    }
                    logger.info("setup presenter");
                    townshipFandomCrawlingProcessFacade.clean();
                }, townshipFandomCrawlingProcessFacade.getTownshipExecutorService());
    }

    /**
     * Process uploaded HTML document.
     *
     * @param uploadedDocument The HTML document from user upload
     * @return CompletableFuture with processing result
     */
    CompletableFuture<Void> asyncProcessFromUploadedHtml(Document uploadedDocument) {
        return townshipFandomCrawlingProcessFacade.processFromUploadedHtml(uploadedDocument)
                .whenCompleteAsync((unused, throwable) -> {
                    if (throwable != null) {
                        logger.error("处理上传文件时出错：{}", throwable.getMessage());
                    }
                    logger.info("上传处理完成");
                    townshipFandomCrawlingProcessFacade.clean();
                }, townshipFandomCrawlingProcessFacade.getTownshipExecutorService());
    }

    void setupTownshipCoordCellGrid(Grid<WikiCrawledParsedCoordCellEntity> grid) {
        grid.setItems(wikiCrawledParsedCoordCellEntityRepository.findAll());
    }

    boolean boolTownshipCrawled() {
        return wikiCrawledEntityRepository.orderByCreatedDateTimeDescLimit1().isPresent();
    }

    public void validateMhtmlHeader(InputStream inputStream) throws IOException {
        this.mhtmlProcessComponent.validateMhtmlHeader(inputStream);
    }

    public Document processUploadedMhtml(InputStream inputStream) throws IOException {
        return this.mhtmlProcessComponent.processUploadedMhtml(inputStream);
    }

    public CompletableFuture<Void> handleUploadSuccess(byte[] data, String fileName) {
        try (var inputStream = new ByteArrayInputStream(data)) {
            // Validate MHTML header
            this.validateMhtmlHeader(inputStream);

            // Reset stream position
            inputStream.reset();

            // Process the uploaded file
            var document = this.processUploadedMhtml(inputStream);;

            // Process the document
            return this.asyncProcessFromUploadedHtml(document);
        }
        catch (Exception e) {
            log.error("处理上传文件时出错", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
