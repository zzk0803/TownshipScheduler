package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.dao.FieldFactoryInfoEntityRepository;
import zzk.townshipscheduler.backend.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.ui.pojo.BillItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
@Getter
@SpringComponent
@UIScope
public class OrderFormPresenter {

    private final OrderEntityRepository orderEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final List<BillItem> gridBillItems = new ArrayList<>();

    private OrderFormView orderFormView;

    private TownshipAuthenticationContext townshipAuthenticationContext;

    private OrderEntity orderEntity;

    private Binder<OrderEntity> binder;

    private ListDataProvider<BillItem> billItemGridDataProvider;

    private GridListDataView<BillItem> billItemGridListDataView;

    private AtomicInteger gridBillItemsCounter = new AtomicInteger();

    public OrderFormPresenter(
            OrderEntityRepository orderEntityRepository,
            ProductEntityRepository productEntityRepository,
            FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository
    ) {
        this.orderEntityRepository = orderEntityRepository;
        this.productEntityRepository = productEntityRepository;
        this.fieldFactoryInfoEntityRepository = fieldFactoryInfoEntityRepository;
    }

    public void refreshItem(BillItem item) {
        billItemGridListDataView.refreshItem(item);
    }

    public void refreshAll() {
        billItemGridListDataView.refreshAll();
    }

    public void addBillItem(BillItem billItem) {
        getGridBillItems().stream()
                .filter(iterating -> iterating.getProductEntity()
                        .getProductId()
                        .equals(billItem.getProductEntity().getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        optionalPresent -> {
                            int amount = optionalPresent.getAmount();
                            optionalPresent.setAmount(amount + billItem.getAmount());
                        }, () -> getGridBillItems().add(billItem)
                );
    }

    OrderFormView getOrderFormView() {
        return orderFormView;
    }

    void setOrderFormView(OrderFormView orderFormView) {
        this.orderFormView = orderFormView;
    }

    void onSubmit() {
        getBinder().validate();
        OrderEntity savingOrder = getOrderEntity();
        savingOrder.setCreatedDateTime(LocalDateTime.now());
        if (!savingOrder.isBoolDeadLine()) {
            savingOrder.setDeadLine(null);
        }

        getGridBillItems()
                .forEach(
                        billItem -> savingOrder.addItem(
                                billItem.getProductEntity(),
                                billItem.getAmount()
                        )
                );


        Optional.ofNullable(getTownshipAuthenticationContext())
                .map(TownshipAuthenticationContext::getUserDetails)
                .map(AccountEntity::getPlayerEntity)
                .ifPresent(player -> {
                    orderEntity.setPlayerEntity(player);
                });
        if (orderEntity.getPlayerEntity() == null) {
            log.error("fail to get player");
        }
        getOrderEntityRepository().saveAndFlush(savingOrder);
    }

    public Binder<OrderEntity> prepareBillAndBinder() {
        this.binder = new Binder<>();
        this.binder.setBean(this.orderEntity = new OrderEntity());
        return this.binder;
    }

    public void setupDataProviderForItems(Grid<BillItem> grid) {
        billItemGridDataProvider = new ListDataProvider<>(getGridBillItems());
        billItemGridListDataView = grid.setItems(getBillItemGridDataProvider());
    }

    public void clean() {
        orderEntity = null;
        gridBillItems.clear();
        gridBillItemsCounter = new AtomicInteger();
    }


    public void setTownshipAuthenticationContext(TownshipAuthenticationContext townshipAuthenticationContext) {
        this.townshipAuthenticationContext = townshipAuthenticationContext;
    }

    public Supplier<Collection<FieldFactoryInfoEntity>> getFactoryProductsSupplier() {
        Optional<PlayerEntity> playerEntity = townshipAuthenticationContext.getPlayerEntity();
        return playerEntity.<Supplier<Collection<FieldFactoryInfoEntity>>>map(entity -> () -> this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                entity.getLevel(),
                Sort.by(Sort.Direction.ASC, "level")
        )).orElseGet(() -> {
            log.error("player should present,but return full entity...");
            return () -> this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                    Sort.by(Sort.Direction.ASC, "level")
            );
        });
    }

    public ProductEntity queryProductById(Long id) {
        return this.productEntityRepository.queryById(id).get();
    }

}
