package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.ui.components.OrderGridItemsCard;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@Route("/orders")
@Menu(title = "Orders", order = 5.00d)
@PermitAll
@Getter
public class OrderListView
        extends VerticalLayout {

    @Serial
    private static final long serialVersionUID = -1639218728062772870L;

    private final OrderListViewPresenter orderListViewPresenter;

    private final Grid<OrderEntity> grid;

    private final ListSignal<OrderEntity> orderListSignal = new ListSignal<>();

    public OrderListView(
            OrderListViewPresenter orderListViewPresenter,
            TownshipAuthenticationContext townshipAuthenticationContext
    ) {
        this.orderListViewPresenter = orderListViewPresenter;
        this.orderListViewPresenter.setView(this);
        this.orderListViewPresenter.setTownshipAuthenticationContext(townshipAuthenticationContext);

        style();

        grid = new Grid<>();
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        grid.addComponentColumn(this::buildBillCard).setFlexGrow(1);
        addAndExpand(grid);

        Signal.effect(
                grid,
                () -> {
                    grid.setItems(orderListSignal.getValues().toList());
                }
        );

        Button addBillButton = new Button(VaadinIcon.PLUS.create());
        addBillButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        addBillButton.setWidth("5rem");
        addBillButton.addClickListener(addBillClicked -> {
            AtomicReference<Dialog> dialogReference = new AtomicReference<>();
            Dialog dialog = new Dialog(
                    new OrderFormView(
                            this,
                            this.orderListViewPresenter,
                            dialogReference
                    )
            );
            dialogReference.set(dialog);
            dialog.setSizeFull();
            dialog.open();
        });
        HorizontalLayout horizontalLayout = new HorizontalLayout(addBillButton);
        horizontalLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        add(horizontalLayout);
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

        if (orderEntity.isBearDeadline()) {
            LocalDateTime deadLine = orderEntity.getDeadLine();
            DateTimePicker dateTimePicker = new DateTimePicker(deadLine);
            dateTimePicker.setLabel("Dead Line");
            dateTimePicker.setReadOnly(true);

            card.add(
                    createCardInnerDiv(
                            strAsSpan(orderEntity.getOrderType().name()),
                            dateTimePicker
                    )
            );
        } else {
            card.add(
                    createCardInnerDiv(
                            strAsSpan(orderEntity.getOrderType().name()),
                            strAsSpan("No Deadline")
                    )
            );
        }

        Scroller scroller = new Scroller(new OrderGridItemsCard(orderEntity));
        scroller.setWidthFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.HORIZONTAL);
        card.addAndExpand(scroller);

        card.add(
                new HorizontalLayout(
                        new Button(
                                VaadinIcon.EDIT.create(),
                                click -> {
                                    Notification.show("todo::edit order");
                                }
                        ),
                        new Button(
                                VaadinIcon.CLOSE.create(),
                                click -> {
                                    this.orderListViewPresenter.removeOrder(orderEntity);
                                    this.orderListViewPresenter.updateOrderListSignal();
                                }
                        )
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (this.getOrderListViewPresenter() != null) {
            this.getOrderListViewPresenter().updateOrderListSignal();
        }
    }

}
