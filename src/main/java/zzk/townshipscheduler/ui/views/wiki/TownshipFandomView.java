package zzk.townshipscheduler.ui.views.wiki;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.persistence.TownshipCoordCell;

@Route(value = "crawling")
@RouteAlias(value = "/")
@RouteAlias(value = "")
@Menu(order = 1d)
public class TownshipFandomView
        extends VerticalLayout {

    public static final String ROUTE = "wiki";

    private final VerticalLayout container;

    private final Button actionButton;

    private final TownshipFandomViewPresenter presenter;

    @Setter
    @Getter
    private UI currentUi;

    public TownshipFandomView(
            TownshipFandomViewPresenter townshipFandomViewPresenter
    ) {
        this.presenter = townshipFandomViewPresenter;
        this.presenter.setProductsView(this);
        setupView();

        container = new VerticalLayout();
        container.setSizeFull();

        actionButton = new Button("Start", VaadinIcon.COFFEE.create());
        actionButton.addClickListener(click -> {
            onActionState();
            presenter.asyncProcess()
                    .whenComplete((unused, throwable) -> {
                                currentUi.access(this::onActionDone);
                            }
                    );
        });
        container.add(actionButton);

        addAndExpand(container);
    }

    private void setupView() {
        addClassName("township-fandom-view");
        setSizeFull();
        setMargin(false);
    }

    public void onActionState() {
        actionButton.setEnabled(false);
    }

    public void onActionDone() {
        actionButton.setEnabled(true);
        container.add(buildShowResultBtn());
    }

    private Button buildShowResultBtn() {
        Button button = new Button("Crawled Result");
        button.addThemeVariants(
                ButtonVariant.LUMO_SUCCESS,
                ButtonVariant.LUMO_LARGE
        );
        button.addClickListener(click -> resultDialogOpen());
        return button;
    }

    private void resultDialogOpen() {
        Dialog dialog = new Dialog();
        dialog.setSizeFull();
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.addComponentAsFirst(prepareCoordCellGrid());
        dialog.getFooter().add(new Button("Close", click -> {
            dialog.close();
        }));
        dialog.open();
    }

    private Grid<TownshipCoordCell> prepareCoordCellGrid() {
        Grid<TownshipCoordCell> grid = new Grid<>(TownshipCoordCell.class);
        grid.setSizeFull();
        this.presenter.setupTownshipCoordCellGrid(grid);
        return grid;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        setCurrentUi(UI.getCurrent());
        if (presenter.boolTownshipCrawled()) {
            container.add(buildShowResultBtn());
        }
    }

}
