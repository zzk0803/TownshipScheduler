package zzk.project.vaadinproject.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.spring.annotation.SpringComponent;
import zzk.project.vaadinproject.backend.persistence.Bill;
import zzk.project.vaadinproject.backend.persistence.BillRepository;
import zzk.project.vaadinproject.ui.form.BillScheduleRequest;

import java.util.List;
import java.util.UUID;

@SpringComponent
class BillViewPresenter {

    private final BillRepository billRepository;

    private BillView view;

    private GridListDataView<Bill> billDataView;

//    private SchedulingService schedulingService;

    public BillViewPresenter(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    BillView getView() {
        return view;
    }

    void setView(BillView view) {
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
        List<Bill> billList = billDataView.getItems().toList();
        billScheduleRequest.setBills(billList);

//        UUID uuid = schedulingService.prepareScheduling(billScheduleRequest);

        //. . .
        return null;
    }

}
