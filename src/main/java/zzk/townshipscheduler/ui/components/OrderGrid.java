package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.jspecify.annotations.Nullable;
import zzk.townshipscheduler.backend.persistence.OrderEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class OrderGrid extends Grid<OrderEntity> {

    private final ComponentEventListener<ClickEvent<Button>> cardDelBtnListener;

    public OrderGrid(Collection<OrderEntity> orders) {
        this.cardDelBtnListener = null;
        this.setSelectionMode(Grid.SelectionMode.NONE);
        this.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        this.addComponentColumn(this::buildBillCard).setFlexGrow(1);
        this.setItems(orders);
    }

    public Component buildBillCard(OrderEntity orderView) {
        HorizontalLayout card = new HorizontalLayout();
        card.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
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

        if (Objects.isNull(this.cardDelBtnListener)) {
            card.add(
                    new Button(VaadinIcon.CLOSE.create())
            );
        }

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

    public OrderGrid(
            Collection<OrderEntity> orders,
            @Nullable ComponentEventListener<ClickEvent<Button>> cardDelBtnListener
    ) {
        this.cardDelBtnListener = cardDelBtnListener;
        this.setSelectionMode(Grid.SelectionMode.NONE);
        this.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        this.addComponentColumn(this::buildBillCard).setFlexGrow(1);
        this.setItems(orders);
    }

    public OrderGrid(List<OrderEntity> orders, boolean boolSuffixBtn) {
        this.cardDelBtnListener = null;
        this.setSelectionMode(Grid.SelectionMode.NONE);
        this.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        this.addComponentColumn(order -> this.buildBillCard(order, false)).setFlexGrow(1);
        this.setItems(orders);
    }

    public Component buildBillCard(OrderEntity orderView, boolean boolSuffixBtn) {
        HorizontalLayout card = new HorizontalLayout();
        card.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
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

        return card;
    }

}
