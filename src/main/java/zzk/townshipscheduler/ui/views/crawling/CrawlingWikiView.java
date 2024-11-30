package zzk.townshipscheduler.ui.views.crawling;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;

@Route(value = "crawling")
@Menu
@AnonymousAllowed
public class CrawlingWikiView extends VerticalLayout {

    private final Button actionButton;

    private final CrawlingWikiViewPresenter presenter;

    @Setter
    @Getter
    private UI currentUi;

    public CrawlingWikiView(
            CrawlingWikiViewPresenter crawlingWikiViewPresenter
    ) {
        this.presenter = crawlingWikiViewPresenter;
        this.presenter.setProductsView(this);
        setupView();


        actionButton = new Button("Start Crawling And Process", VaadinIcon.PLAY.create());
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionButton.setDisableOnClick(true);
        actionButton.addClickListener(click -> {
            presenter.asyncProcess()
                    .whenComplete((unused, throwable) -> {
                                currentUi.access(()-> add(prepareCoordCellGrid()));
                            }
                    );
        });

        add(actionButton);
    }

    private void setupView() {
        addClassName("township-fandom-view");
        setSizeFull();
        setMargin(false);
    }


    public void onActionDone() {
        actionButton.setEnabled(true);
    }


    private Grid<WikiCrawledParsedCoordCellEntity> prepareCoordCellGrid() {
        Grid<WikiCrawledParsedCoordCellEntity> grid = new Grid<>(WikiCrawledParsedCoordCellEntity.class);
        grid.setWidthFull();
        presenter.setupTownshipCoordCellGrid(grid);
        return grid;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        setCurrentUi(UI.getCurrent());
        if (presenter.boolTownshipCrawled()) {
            actionButton.setEnabled(false);
            add(prepareCoordCellGrid());
        }
    }

}
