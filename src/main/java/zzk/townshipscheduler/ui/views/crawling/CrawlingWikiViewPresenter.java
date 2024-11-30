package zzk.townshipscheduler.ui.views.crawling;

import com.vaadin.flow.component.grid.Grid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zzk.townshipscheduler.backend.crawling.TownshipFandomCrawlingProcessFacade;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledEntityRepository;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledParsedCoordCellEntityRepository;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class CrawlingWikiViewPresenter {

    public static final Logger logger = LoggerFactory.getLogger(CrawlingWikiViewPresenter.class);

    private final TownshipFandomCrawlingProcessFacade townshipFandomCrawlingProcessFacade;

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
                }, townshipFandomCrawlingProcessFacade.getTownshipExecutorService());
    }

    void setupTownshipCoordCellGrid(Grid<WikiCrawledParsedCoordCellEntity> grid) {
        grid.setItems(wikiCrawledParsedCoordCellEntityRepository.findAll());
    }

    boolean boolTownshipCrawled() {
        return wikiCrawledEntityRepository.orderByCreatedDateTimeDescLimit1().isPresent();
    }

}
