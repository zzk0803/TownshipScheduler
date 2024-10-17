package zzk.project.vaadinproject.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import zzk.project.vaadinproject.backend.persistence.Bill;
import zzk.project.vaadinproject.backend.persistence.BillRepository;
import zzk.project.vaadinproject.ui.form.BillItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@SpringComponent
class BillFormPresenter {

    private final BillRepository billRepository;

    private final List<BillItem> gridBillItems = new ArrayList<>();

    private BillForm billForm;

    private Bill bill;

    private Binder<Bill> binder;

    private ListDataProvider<BillItem> billItemGridDataProvider;

    private GridListDataView<BillItem> billItemGridListDataView;

    private AtomicInteger gridBillItemsCounter = new AtomicInteger();


    public BillFormPresenter(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public void refreshItem(BillItem item) {
        billItemGridListDataView.refreshItem(item);
    }

    public void refreshAll() {
        billItemGridListDataView.refreshAll();
    }

    public void addBillItem(BillItem billItem) {
        getGridBillItems().add(billItem);
    }

    BillForm getBillForm() {
        return billForm;
    }

    void setBillForm(BillForm billForm) {
        this.billForm = billForm;
    }

    void onSubmit() {
        getBinder().validate();
        getBill().setCreatedDateTime(LocalDateTime.now());

        assert getBillItemGridDataProvider().getItems() == getGridBillItems();
        assert getBillItemGridDataProvider().getItems().equals(getGridBillItems());
        getGridBillItems().forEach(billItem -> getBill().addItem(billItem.getGoods(), billItem.getAmount()));

        getBillRepository().saveAndFlush(getBill());
    }

    public Binder<Bill> prepareBillAndBinder() {
        this.binder = new Binder<>();
        this.binder.setBean(this.bill = new Bill());
        return getBinder();
    }

    public void setupDataProviderForItems(Grid<BillItem> grid) {
        billItemGridDataProvider = new ListDataProvider<>(getGridBillItems());
        billItemGridListDataView = grid.setItems(getBillItemGridDataProvider());
    }

    public void clean() {
        bill = null;
        gridBillItems.clear();
        gridBillItemsCounter = new AtomicInteger();
    }


}
