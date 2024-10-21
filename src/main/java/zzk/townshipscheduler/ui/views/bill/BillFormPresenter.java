package zzk.townshipscheduler.ui.views.bill;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import zzk.townshipscheduler.backend.persistence.Bill;
import zzk.townshipscheduler.backend.persistence.BillRepository;
import zzk.townshipscheduler.adopting.form.BillItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@SpringComponent
class BillFormPresenter {

    private final BillRepository billRepository;

    private final List<BillItem> gridBillItems = new ArrayList<>();

    private BillFormView billFormView;

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

    BillFormView getBillFormView() {
        return billFormView;
    }

    void setBillFormView(BillFormView billFormView) {
        this.billFormView = billFormView;
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
