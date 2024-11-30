package zzk.townshipscheduler.ui.views.orders;

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
import jakarta.annotation.security.PermitAll;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.pojo.form.BillScheduleRequest;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingView;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Route("/orders")
@Menu
@PermitAll
public class OrderListView extends VerticalLayout implements AfterNavigationObserver {

    private final OrderListViewPresenter orderListViewPresenter;

    private final Grid<OrderEntity> grid = new Grid<>();

    public OrderListView(
            OrderListViewPresenter orderListViewPresenter
    ) {
        this.orderListViewPresenter = orderListViewPresenter;
        orderListViewPresenter.setView(this);

        style();

        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        grid.addComponentColumn(this::buildBillCard);
        addAndExpand(grid);

        Button schedulingButton = new Button(VaadinIcon.TIMER.create());
        schedulingButton.addClickListener(click -> {
            GridDataView<OrderEntity> genericDataView = grid.getGenericDataView();
            Stream<OrderEntity> orderEntityStream = genericDataView.getItems();
            UUID uuid = orderListViewPresenter.dealBillScheduleRequest(
                    BillScheduleRequest.builder()
                            .orderEntities(orderEntityStream.toList())
                            .build()
            );
            UI.getCurrent().navigate(SchedulingView.class, uuid.toString());
        });

        Button addBillButton = new Button(VaadinIcon.PLUS.create());
        addBillButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        addBillButton.setWidth("5rem");
        addBillButton.addClickListener(addBillClicked -> UI.getCurrent().navigate(OrderFormView.class));
        add(new HorizontalLayout(addBillButton, schedulingButton));
    }

    private void style() {
        addClassName("bill-view");
        setMargin(false);
    }

    public Component buildBillCard(OrderEntity orderEntity) {
        HorizontalLayout card = new HorizontalLayout();
        card.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        card.addClassNames("card");
        card.getThemeList().add("space-s");

        boolean boolDeadLine = orderEntity.isBoolDeadLine();
        if (boolDeadLine) {
            LocalDateTime deadLine = orderEntity.getDeadLine();
            DateTimePicker dateTimePicker = new DateTimePicker(deadLine);
            dateTimePicker.setLabel("Dead Line");
            dateTimePicker.setReadOnly(true);
            card.add(new VerticalLayout(strAsSpan(orderEntity.getOrderType().name()), dateTimePicker));
        } else {
            card.add(new VerticalLayout(strAsSpan(orderEntity.getOrderType().name()), strAsSpan("No Deadline")));
        }

        card.add(buildBillItemPairsComponent(orderEntity));

        card.add(new Button(VaadinIcon.CLOSE.create(), click -> {
            orderListViewPresenter.onBillDeleteClick(orderEntity);
        }));

        return card;
    }

    public Span strAsSpan(String content) {
        return new Span(content);
    }

    private HorizontalLayout buildBillItemPairsComponent(OrderEntity orderEntity) {
        HorizontalLayout billItemLayout = new HorizontalLayout();
        Map<ProductEntity, Integer> productAmountPairs = orderEntity.getProductAmountPairs();
        productAmountPairs.forEach((goods, integer) -> {
            Image image = new Image();
            image.setHeight("40px");
            image.setWidth("40px");
            image.setSrc(
                    new StreamResource(
                            goods.getName(),
                            () -> new ByteArrayInputStream(goods.getCrawledAsImage().getImageBytes())
                    )
            );

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

    private void fillBillCards(Grid<OrderEntity> grid) {
        orderListViewPresenter.fillGrid(grid);
    }

}
