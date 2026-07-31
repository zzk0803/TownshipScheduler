package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.TaskScheduler;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntityRepository;
import zzk.townshipscheduler.backend.persistence.OrderEntityRepository;
import zzk.townshipscheduler.backend.persistence.PlayerEntityRepository;
import zzk.townshipscheduler.backend.persistence.ProductEntityRepository;
import zzk.townshipscheduler.backend.PlayerService;
import zzk.townshipscheduler.ui.components.ProductImages;
import zzk.townshipscheduler.ui.components.ProductImagesBytesComponent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Setter
@Getter
@SpringComponent
@UIScope
public class OrderListViewPresenter {

    private final OrderEntityRepository orderEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final PlayerService playerService;

    private final ProductImagesBytesComponent productImagesBytesComponent;

    private final TaskScheduler taskScheduler;

    private TownshipAuthenticationContext townshipAuthenticationContext;

    private OrderListView view;

    public OrderListViewPresenter(
            OrderEntityRepository orderEntityRepository,
            ProductEntityRepository productEntityRepository,
            FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository,
            PlayerService playerService,
            ProductImagesBytesComponent productImagesBytesComponent,
            @Qualifier("townshipTaskScheduler") TaskScheduler taskScheduler
    ) {

        this.orderEntityRepository = orderEntityRepository;
        this.productEntityRepository = productEntityRepository;
        this.fieldFactoryInfoEntityRepository = fieldFactoryInfoEntityRepository;
        this.playerService = playerService;
        this.productImagesBytesComponent = productImagesBytesComponent;
        this.taskScheduler = taskScheduler;
    }

    public void removeOrder(OrderEntity orderEntity) {
        Optional<PlayerEntity> optionalPlayerEntity = townshipAuthenticationContext.getPlayerEntity();
        PlayerEntity playerEntity = optionalPlayerEntity.orElseThrow();
        playerEntity.removeOrderEntity(orderEntity);
        PlayerEntityRepository playerEntityRepository = playerService.getPlayerEntityRepository();
        playerEntityRepository.saveAndFlush(playerEntity);
        orderEntityRepository.deleteById(orderEntity.getId());
    }

    public void updateOrderListSignal() {
        List<OrderEntity> orderEntities = queryBillList();
        ListSignal<OrderEntity> viewOrdersSignal = getView().getOrderListSignal();
        viewOrdersSignal.clear();
        viewOrdersSignal.insertAllLast(orderEntities);
    }

    private List<OrderEntity> queryBillList() {
        Optional<PlayerEntity> optionalPlayer = townshipAuthenticationContext.getPlayerEntity();
        PlayerEntity player = optionalPlayer.orElseThrow();
        return orderEntityRepository.queryForOrderListView(player);
    }

    OrderListView getView() {
        return view;
    }

    void setView(OrderListView view) {
        this.view = view;
    }

    public Supplier<Collection<FieldFactoryInfoEntity>> getFieldFactoryInfoCollectionSupplier() {
        if (getTownshipAuthenticationContext() != null && getTownshipAuthenticationContext().getPlayerEntity().isPresent()) {
            return getFieldFactoryInfoCollectionSupplier(getTownshipAuthenticationContext().getPlayerEntity().get());
        }
        return () -> this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                Sort.by(
                        Sort.Direction.ASC,
                        "level"
                )
        );
    }

    public Supplier<Collection<FieldFactoryInfoEntity>> getFieldFactoryInfoCollectionSupplier(PlayerEntity player) {
        return () -> this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                player.getLevel(),
                Sort.by(
                        Sort.Direction.ASC,
                        "level"
                )
        );
    }

    public Image productImage(ProductEntity productEntity) {
        byte[] productImage = this.productImagesBytesComponent.getProductImage(productEntity);
        return ProductImages.productImage(productEntity.getName(), productImage);
    }

}
