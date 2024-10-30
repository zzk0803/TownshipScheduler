package zzk.townshipscheduler.ui.views.bill;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.spring.annotation.SpringComponent;
import zzk.townshipscheduler.backend.persistence.BillRepository;
import zzk.townshipscheduler.backend.persistence.Order;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.port.form.BillScheduleRequest;

import java.util.List;
import java.util.UUID;

@SpringComponent
class BillListViewPresenter {

    private final BillRepository billRepository;

    private final ITownshipSchedulingService schedulingService;

    private BillListView view;

    private GridListDataView<Order> billDataView;

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

    void onBillDeleteClick(Order order) {
        billRepository.delete(order);
        billDataView.removeItem(order);
        this.view.onBillDeleteDone();
    }

    public void fillGrid(Grid<Order> grid) {
        billDataView = grid.setItems(queryBillList());
    }

    private List<Order> queryBillList() {
        return billRepository.findBy(Order.class);
    }

    public UUID dealBillScheduleRequest(BillScheduleRequest billScheduleRequest) {
        return schedulingService.prepareScheduling(billScheduleRequest);
    }

}
