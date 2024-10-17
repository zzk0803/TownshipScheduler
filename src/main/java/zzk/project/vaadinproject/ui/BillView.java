package zzk.project.vaadinproject.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import zzk.project.vaadinproject.backend.persistence.Bill;
import zzk.project.vaadinproject.backend.persistence.Goods;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Map;

@Route
@Menu(order = 3d)
public class BillView
        extends VerticalLayout
        implements AfterNavigationObserver {

    public static final String ROUTE = "bills";

    private final BillViewPresenter billViewPresenter;

    private final Grid<Bill> grid = new Grid<>();

    public BillView(
            BillViewPresenter billViewPresenter
    ) {
        this.billViewPresenter = billViewPresenter;
        billViewPresenter.setView(this);
        addClassName("bill-view");
        setMargin(false);

        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        grid.addComponentColumn(this::buildBillCard);
        addAndExpand(grid);

        Button addBillButton = new Button(VaadinIcon.PLUS.create());
        addBillButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        addBillButton.setWidth("5rem");
        addBillButton.addClickListener(addBillClicked -> {
            UI.getCurrent().navigate(BillForm.class);
        });
        add(addBillButton);
    }

    public Component buildBillCard(Bill bill) {
        HorizontalLayout card = new HorizontalLayout();
        card.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        card.addClassNames("card");
        card.getThemeList().add("space-s");

        boolean boolDeadLine = bill.isBoolDeadLine();
        if (boolDeadLine) {
            LocalDateTime deadLine = bill.getDeadLine();
            DateTimePicker dateTimePicker = new DateTimePicker(deadLine);
            dateTimePicker.setLabel("Dead Line");
            dateTimePicker.setReadOnly(true);
            card.add(new VerticalLayout(strAsSpan(bill.getBillType().name()), dateTimePicker));
        } else {
            card.add(new VerticalLayout(strAsSpan(bill.getBillType().name()), strAsSpan("No Deadline")));
        }

        card.add(buildBillItemPairsComponent(bill));

        card.add(new Button(VaadinIcon.CLOSE.create(), click -> {
            billViewPresenter.onBillDeleteClick(bill);
        }));

        return card;
    }

    public Span strAsSpan(String content) {
        return new Span(content);
    }

    private HorizontalLayout buildBillItemPairsComponent(Bill bill) {
        HorizontalLayout billItemLayout = new HorizontalLayout();
        Map<Goods, Integer> productAmountPairs = bill.getProductAmountPairs();
        productAmountPairs.forEach((goods, integer) -> {
            Image image = new Image();
            image.setHeight("40px");
            image.setWidth("40px");
            image.setSrc(new StreamResource(
                    goods.getName(),
                    () -> new ByteArrayInputStream(goods.getImageBytes())
            ));

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

    private void fillBillCards(Grid<Bill> grid) {
        billViewPresenter.fillGrid(grid);
    }

}
