package zzk.townshipscheduler.ui.views.bill;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.spring.annotation.SpringComponent;
import zzk.townshipscheduler.backend.persistence.Bill;
import zzk.townshipscheduler.backend.persistence.BillRepository;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.adopting.form.BillScheduleRequest;

import java.util.List;
import java.util.UUID;

@SpringComponent
class BillListViewPresenter {

    private final BillRepository billRepository;

    private final ITownshipSchedulingService schedulingService;

    private BillListView view;

    private GridListDataView<Bill> billDataView;

//    private SchedulingService schedulingService;

    public BillListViewPresenter(BillRepository billRepository, ITownshipSchedulingService schedulingService) {
        this.billRepository = billRepository;
        this.schedulingService = schedulingService;
    }

    BillListView getView() {
        return view;
    }

    void setView(BillListView view) {
        this.view = view;
    }

    void onBillDeleteClick(Bill bill) {
        billRepository.delete(bill);
        billDataView.removeItem(bill);
        this.view.onBillDeleteDone();
    }

    public void fillGrid(Grid<Bill> grid) {
        billDataView = grid.setItems(queryBillList());
    }

    private List<Bill> queryBillList() {
        return billRepository.findBy(Bill.class);
    }

    public UUID dealBillScheduleRequest(BillScheduleRequest billScheduleRequest) {
        UUID uuid = schedulingService.prepareScheduling(billScheduleRequest);

        //. . .
        return null;
    }

}
