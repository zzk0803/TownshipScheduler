package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingPrepareComponent;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingRequest;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.backend.service.PlayerService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringComponent
public class OrderListViewPresenter {

    private final OrderEntityRepository orderEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final ITownshipSchedulingService schedulingService;

    private final PlayerService playerService;

    private final TownshipSchedulingPrepareComponent townshipSchedulingPrepareComponent;

    private final TransactionTemplate transactionTemplate;

    private OrderListView view;

    @Getter
    @Setter
    private TownshipAuthenticationContext townshipAuthenticationContext;

    private GridListDataView<OrderEntity> billDataView;

    public OrderListViewPresenter(
            OrderEntityRepository orderEntityRepository,
            ProductEntityRepository productEntityRepository,
            ITownshipSchedulingService schedulingService,
            PlayerService playerService,
            TownshipSchedulingPrepareComponent townshipSchedulingPrepareComponent, TransactionTemplate transactionTemplate
    ) {

        this.orderEntityRepository = orderEntityRepository;
        this.productEntityRepository = productEntityRepository;
        this.schedulingService = schedulingService;
        this.playerService = playerService;
        this.townshipSchedulingPrepareComponent = townshipSchedulingPrepareComponent;
        this.transactionTemplate = transactionTemplate;
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

//    public UUID buildTownshipSchedulingRequest() {
//        TownshipSchedulingRequest townshipSchedulingRequest
//                = prepareSchedulingService.buildTownshipSchedulingRequest();
//        TownshipSchedulingProblem problem
//                = schedulingService.prepareScheduling(townshipSchedulingRequest);
//        return problem.getUuid();
//    }

    public UUID backendPrepareTownshipScheduling(TownshipAuthenticationContext townshipAuthenticationContext) {
        return transactionTemplate.execute(status -> {
            TownshipSchedulingRequest townshipSchedulingRequest
                    = townshipSchedulingPrepareComponent.buildTownshipSchedulingRequest(townshipAuthenticationContext);
            TownshipSchedulingProblem problem
                    = schedulingService.prepareScheduling(townshipSchedulingRequest);
            return problem.getUuid();
        });
    }


}
