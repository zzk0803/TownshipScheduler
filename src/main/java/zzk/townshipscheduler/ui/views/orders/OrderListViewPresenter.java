package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.TaskScheduler;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.persistence.dao.FieldFactoryInfoEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.service.PlayerService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Setter
@Getter
@SpringComponent
public class OrderListViewPresenter {

    private final OrderEntityRepository orderEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final PlayerService playerService;

    private final TaskScheduler taskScheduler;

    private TownshipAuthenticationContext townshipAuthenticationContext;

    private GridListDataView<OrderEntity> billDataView;

    private OrderListView view;

    public OrderListViewPresenter(
            OrderEntityRepository orderEntityRepository,
            ProductEntityRepository productEntityRepository,
            FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository,
            PlayerService playerService,
            @Qualifier("townshipTaskScheduler") TaskScheduler taskScheduler
    ) {

        this.orderEntityRepository = orderEntityRepository;
        this.productEntityRepository = productEntityRepository;
        this.fieldFactoryInfoEntityRepository = fieldFactoryInfoEntityRepository;
        this.playerService = playerService;
        this.taskScheduler = taskScheduler;
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

    public void updateOrderListSignal() {
        List<OrderEntity> orderEntities = queryBillList();
        getView().getOrderListSignal().clear();
        getView().getOrderListSignal().insertAllLast(orderEntities);
    }

    public Supplier<Collection<FieldFactoryInfoEntity>> getCollectionSupplier() {
        if (getTownshipAuthenticationContext() != null && getTownshipAuthenticationContext().getPlayerEntity().isPresent()) {
            return getCollectionSupplier(getTownshipAuthenticationContext().getPlayerEntity().get());
        }
        return () -> this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                Sort.by(
                        Sort.Direction.ASC,
                        "level"
                )
        );
    }

    public Supplier<Collection<FieldFactoryInfoEntity>> getCollectionSupplier(PlayerEntity player) {
        return () -> this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                player.getLevel(),
                Sort.by(
                        Sort.Direction.ASC,
                        "level"
                )
        );
    }

}
