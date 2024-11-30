package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.spring.annotation.SpringComponent;
import zzk.townshipscheduler.backend.persistence.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.pojo.form.BillScheduleRequest;

import java.util.List;
import java.util.UUID;

@SpringComponent
public class OrderListViewPresenter {

    private final OrderEntityRepository orderEntityRepository;

    private final ITownshipSchedulingService schedulingService;

    private OrderListView view;

    private GridListDataView<OrderEntity> billDataView;

//    private SchedulingService schedulingService;

    public OrderListViewPresenter(OrderEntityRepository orderEntityRepository, ITownshipSchedulingService schedulingService) {
        this.orderEntityRepository = orderEntityRepository;
        this.schedulingService = schedulingService;
    }

    OrderListView getView() {
        return view;
    }

    void setView(OrderListView view) {
        this.view = view;
    }

    void onBillDeleteClick(OrderEntity orderEntity) {
        orderEntityRepository.delete(orderEntity);
        billDataView.removeItem(orderEntity);
        this.view.onBillDeleteDone();
    }

    public void fillGrid(Grid<OrderEntity> grid) {
        billDataView = grid.setItems(queryBillList());
    }

    private List<OrderEntity> queryBillList() {
        return orderEntityRepository.findBy(OrderEntity.class);
    }

    public UUID dealBillScheduleRequest(BillScheduleRequest billScheduleRequest) {
        return schedulingService.prepareScheduling(billScheduleRequest);
    }

}
