package zzk.townshipscheduler.ui.views.wiki;

import com.vaadin.flow.component.grid.Grid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zzk.townshipscheduler.backend.crawling.CrawlingFacade;
import zzk.townshipscheduler.backend.persistence.TownshipCoordCell;
import zzk.townshipscheduler.backend.persistence.TownshipCoordCellRepository;
import zzk.townshipscheduler.backend.persistence.TownshipCrawledRepository;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
class TownshipFandomViewPresenter {

    public static final Logger logger = LoggerFactory.getLogger(TownshipFandomViewPresenter.class);

    private final CrawlingFacade crawlingFacade;

    private final TownshipCrawledRepository townshipCrawledRepository;

    private final TownshipCoordCellRepository townshipCoordCellRepository;

    private TownshipFandomView view;

    void setProductsView(TownshipFandomView townshipFandomView) {
        this.view = townshipFandomView;
    }

    CompletableFuture<Void> asyncProcess() {
        return crawlingFacade.process()
                .whenCompleteAsync((unused, throwable) -> {
                    if (throwable != null) {
                        logger.error(throwable.getMessage());
                        throwable.printStackTrace();
                    }
                    logger.info("setup presenter");
                }, crawlingFacade.getTownshipExecutorService());
    }

    void setupTownshipCoordCellGrid(Grid<TownshipCoordCell> grid) {
        grid.setItems(townshipCoordCellRepository.findAll());
    }

    boolean boolTownshipCrawled() {
        return townshipCrawledRepository.orderByCreatedDateTimeDescLimit1().isPresent();
    }

}
