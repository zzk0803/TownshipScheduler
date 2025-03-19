package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.ui.components.BillCard;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingView;

import java.time.LocalDateTime;
import java.util.UUID;

@Route("/orders")
@Menu
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
        grid.addComponentColumn(this::buildBillCard);
        addAndExpand(grid);

        Button schedulingButton = new Button(VaadinIcon.TIMER.create());
        schedulingButton.addClickListener(click -> {
            var uuid = presenter.backendPrepareTownshipScheduling(townshipAuthenticationContext);
            UI.getCurrent().navigate(SchedulingView.class, uuid);
        });

        Button addBillButton = new Button(VaadinIcon.PLUS.create());
        addBillButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        addBillButton.setWidth("5rem");
        addBillButton.addClickListener(addBillClicked -> UI.getCurrent().navigate(OrderFormView.class));
        HorizontalLayout horizontalLayout = new HorizontalLayout(addBillButton, schedulingButton);
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
            card.add(new VerticalLayout(strAsSpan(orderView.getOrderType().name()), dateTimePicker));
        } else {
            card.add(new VerticalLayout(strAsSpan(orderView.getOrderType().name()), strAsSpan("No Deadline")));
        }

        card.add(new BillCard(orderView));

        card.add(new Button(
                VaadinIcon.CLOSE.create(), click -> {
            presenter.onBillDeleteClick(orderView);
        }
        ));

        return card;
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
