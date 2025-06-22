package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.ui.components.BillCard;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingView;

import java.time.LocalDateTime;
import java.util.Arrays;

@Route("/orders")
@Menu(title = "Orders", order = 5.00d)
@PermitAll
public class OrderListView extends VerticalLayout implements AfterNavigationObserver {

    private final OrderListViewPresenter presenter;

    private final Grid<OrderEntity> grid = new Grid<>();

    public OrderListView(
            OrderListViewPresenter presenter,
            TownshipAuthenticationContext townshipAuthenticationContext
    ) {
        this.presenter = presenter;
        presenter.setView(this);
        presenter.setTownshipAuthenticationContext(townshipAuthenticationContext);

        style();

        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        grid.addComponentColumn(this::buildBillCard).setFlexGrow(1);
        addAndExpand(grid);


        Button addBillButton = new Button(VaadinIcon.PLUS.create());
        addBillButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        addBillButton.setWidth("5rem");
        addBillButton.addClickListener(addBillClicked -> UI.getCurrent().navigate(OrderFormView.class));
        HorizontalLayout horizontalLayout = new HorizontalLayout(addBillButton);
        horizontalLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        add(horizontalLayout);
    }

    private void style() {
        addClassName("bill-view");
        setMargin(false);
    }

    public Component buildBillCard(OrderEntity orderView) {
        HorizontalLayout card = new HorizontalLayout();
        card.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        card.addClassNames("card");
        card.getThemeList().add("space-s");

        boolean boolDeadLine = orderView.isBoolDeadLine();
        if (boolDeadLine) {
            LocalDateTime deadLine = orderView.getDeadLine();
            DateTimePicker dateTimePicker = new DateTimePicker(deadLine);
            dateTimePicker.setLabel("Dead Line");
            dateTimePicker.setReadOnly(true);

            card.add(
                    createCardInnerDiv(
                            strAsSpan(orderView.getOrderType().name()),
                            dateTimePicker
                    )
            );
        } else {
            card.add(
                    createCardInnerDiv(
                            strAsSpan(orderView.getOrderType().name()),
                            strAsSpan("No Deadline")
                    )
            );
        }

        Scroller scroller = new Scroller(new BillCard(orderView));
        scroller.setWidthFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.HORIZONTAL);
        card.addAndExpand(scroller);

        card.add(
                new Button(
                        VaadinIcon.CLOSE.create(),
                        click -> {
                            presenter.onBillDeleteClick(orderView);
                        }
                )
        );

        return card;
    }

    public Div createCardInnerDiv(Component... components) {
        Div div = new Div();
        div.addClassNames(
                LumoUtility.Width.AUTO,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.MEDIUM
        );
        Arrays.stream(components).forEach(div::add);
        return div;
    }

    public Span strAsSpan(String content) {
        return new Span(content);
    }


    public void onBillDeleteDone() {
        grid.getDataProvider().refreshAll();
        grid.getListDataView().refreshAll();
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        presenter.fillGrid(grid);
    }

}
