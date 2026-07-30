package zzk.townshipscheduler.ui.views.crawling;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import zzk.townshipscheduler.backend.crawling.MhtmlProcessComponent;
import zzk.townshipscheduler.backend.crawling.TownshipFandomCrawlingProcessFacade;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledParsedCoordCellEntityRepository;

import java.time.Duration;
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
        return townshipFandomCrawlingProcessFacade.process()
                .whenCompleteAsync(
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
        return wikiCrawledEntityRepository.orderByCreatedDateTimeDescLimit1()
                .isPresent();
    }

    CompletableFuture<Void> asyncProcessFromOfflineHtml() {
        return townshipFandomCrawlingProcessFacade.processFromOfflineMhtmlAsTxt()
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

    public InMemoryUploadHandler createUploadHandler() {
        InMemoryUploadHandler uploadHandler = UploadHandler.inMemory(
                (metadata, data) -> {
                    // Get other information about the file.
                    String fileName = metadata.fileName();
                    String mimeType = metadata.contentType();
                    long contentLength = metadata.contentLength();

                    this.view.getCurrentUi()
                            .access(() -> {
                                Notification.show("File Received！Processing...", 3000, Notification.Position.TOP_CENTER);
                            });

                    // Do something with the file data...
                    MhtmlProcessComponent.Result mhtmlResult = processMhtmlBytes(data);
                    asyncProcessFromUploadedHtml(mhtmlResult)
                            .whenComplete(
                                    (unused, throwable) -> {
                                        this.view.getCurrentUi()
                                                .access(() -> {
                                                    if (throwable == null) {
                                                        this.view.add(this.view.prepareCoordCellGrid());
                                                        Notification.show("Done!", 5000, Notification.Position.BOTTOM_CENTER);
                                                    } else {
                                                        Notification.show("Fail!" + throwable.getMessage(), 8000, Notification.Position.BOTTOM_CENTER);
                                                    }
                                                });
                                    }
                            )
                            .exceptionally(throwable -> {
                                this.view.getCurrentUi()
                                        .access(() -> {
                                            Notification notification = new Notification("Error occur when get data from fandom wiki");
                                            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                                            notification.setPosition(Notification.Position.MIDDLE);
                                            notification.setDuration(Duration.ofSeconds(3)
                                                    .toSecondsPart());
                                            notification.open();
                                            this.view.getActionButton()
                                                    .setDisableOnClick(false);
                                        });
                                return null;
                            });
                }
        );
        return uploadHandler;
    }

    public MhtmlProcessComponent.Result processMhtmlBytes(byte[] data) {
        return this.mhtmlProcessComponent.processMhtmlBytes(data);
    }

    /**
     * Process uploaded HTML document.
     *
     * @param uploadedDocument The HTML document from user upload
     * @return CompletableFuture with processing result
     */
    CompletableFuture<Void> asyncProcessFromUploadedHtml(MhtmlProcessComponent.Result uploadedDocument) {
        return townshipFandomCrawlingProcessFacade.processFromOfflineMhtmlAsTxt(uploadedDocument)
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
