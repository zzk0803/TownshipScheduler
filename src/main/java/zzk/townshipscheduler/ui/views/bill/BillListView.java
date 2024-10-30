package zzk.townshipscheduler.ui.views.bill;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridDataView;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.backend.persistence.Order;
import zzk.townshipscheduler.port.form.BillScheduleRequest;
import zzk.townshipscheduler.ui.views.schedule.ScheduleView;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Route
@Menu(order = 3d)
public class BillListView extends VerticalLayout implements AfterNavigationObserver {

    private final BillListViewPresenter billListViewPresenter;

    private final Grid<Order> grid = new Grid<>();

    public BillListView(
            BillListViewPresenter billListViewPresenter
    ) {
        this.billListViewPresenter = billListViewPresenter;
        billListViewPresenter.setView(this);

        style();

        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        grid.addComponentColumn(this::buildBillCard);
        addAndExpand(grid);

        Button schedulingButton = new Button();
        schedulingButton.addClickListener(click -> {
            GridDataView<Order> genericDataView = grid.getGenericDataView();
            Stream<Order> items = genericDataView.getItems();
            UUID uuid = billListViewPresenter.dealBillScheduleRequest(BillScheduleRequest.builder().orders(items.toList()).build());
            UI.getCurrent().navigate(ScheduleView.class, uuid.toString());
        });

        Button addBillButton = new Button(VaadinIcon.PLUS.create());
        addBillButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        addBillButton.setWidth("5rem");
        addBillButton.addClickListener(addBillClicked -> UI.getCurrent().navigate(BillFormView.class));
        add(addBillButton);
    }

    private void style() {
        addClassName("bill-view");
        setMargin(false);
    }

    public Component buildBillCard(Order order) {
        HorizontalLayout card = new HorizontalLayout();
        card.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        card.addClassNames("card");
        card.getThemeList().add("space-s");

        boolean boolDeadLine = order.isBoolDeadLine();
        if (boolDeadLine) {
            LocalDateTime deadLine = order.getDeadLine();
            DateTimePicker dateTimePicker = new DateTimePicker(deadLine);
            dateTimePicker.setLabel("Dead Line");
            dateTimePicker.setReadOnly(true);
            card.add(new VerticalLayout(strAsSpan(order.getOrderType().name()), dateTimePicker));
        } else {
            card.add(new VerticalLayout(strAsSpan(order.getOrderType().name()), strAsSpan("No Deadline")));
        }

        card.add(buildBillItemPairsComponent(order));

        card.add(new Button(VaadinIcon.CLOSE.create(), click -> {
            billListViewPresenter.onBillDeleteClick(order);
        }));

        return card;
    }

    public Span strAsSpan(String content) {
        return new Span(content);
    }

    private HorizontalLayout buildBillItemPairsComponent(Order order) {
        HorizontalLayout billItemLayout = new HorizontalLayout();
        Map<Goods, Integer> productAmountPairs = order.getProductAmountPairs();
        productAmountPairs.forEach((goods, integer) -> {
            Image image = new Image();
            image.setHeight("40px");
            image.setWidth("40px");
            image.setSrc(new StreamResource(goods.getName(), () -> new ByteArrayInputStream(goods.getImageBytes())));

            Span span = new Span(goods.getName());

            VerticalLayout leftImageAndTextVL = new VerticalLayout(image, span);
            leftImageAndTextVL.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

            HorizontalLayout itemAndAmountHL = new HorizontalLayout(leftImageAndTextVL, new Span("x" + integer));
            itemAndAmountHL.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            itemAndAmountHL.setSpacing(false);
            itemAndAmountHL.setMargin(false);
            billItemLayout.add(itemAndAmountHL);
        });
        return billItemLayout;
    }

    public void onBillDeleteDone() {
        grid.getDataProvider().refreshAll();
        grid.getListDataView().refreshAll();
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        fillBillCards(grid);
    }

    private void fillBillCards(Grid<Order> grid) {
        billListViewPresenter.fillGrid(grid);
    }

}
