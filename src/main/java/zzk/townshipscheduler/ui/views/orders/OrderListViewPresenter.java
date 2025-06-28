package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.service.PlayerService;

import java.util.List;
import java.util.Optional;

@SpringComponent
public class OrderListViewPresenter {

    private final OrderEntityRepository orderEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final PlayerService playerService;

    private OrderListView view;

    @Getter
    @Setter
    private TownshipAuthenticationContext townshipAuthenticationContext;

    private GridListDataView<OrderEntity> billDataView;

    public OrderListViewPresenter(
            OrderEntityRepository orderEntityRepository,
            ProductEntityRepository productEntityRepository,
            PlayerService playerService
    ) {

        this.orderEntityRepository = orderEntityRepository;
        this.productEntityRepository = productEntityRepository;
        this.playerService = playerService;
    }

    OrderListView getView() {
        return view;
    }

    void setView(OrderListView view) {
        this.view = view;
    }

    void onBillDeleteClick(OrderEntity orderEntity) {
        orderEntityRepository.deleteById(orderEntity.getId());
        billDataView.removeItem(orderEntity);
        this.view.onBillDeleteDone();
    }

    public void fillGrid(Grid<OrderEntity> grid) {
        List<OrderEntity> orderEntities = queryBillList();
        billDataView = grid.setItems(orderEntities);
    }

    private List<OrderEntity> queryBillList() {
        Optional<PlayerEntity> optionalPlayer = townshipAuthenticationContext.getPlayerEntity();
        PlayerEntity player = optionalPlayer.orElseThrow();
        return orderEntityRepository.queryForOrderListView(player);
    }

}
