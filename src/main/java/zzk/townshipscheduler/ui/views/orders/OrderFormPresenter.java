package zzk.townshipscheduler.ui.views.orders;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.ui.pojo.BillItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
@SpringComponent
public class OrderFormPresenter {

    private final OrderEntityRepository orderEntityRepository;

    private final List<BillItem> gridBillItems = new ArrayList<>();

    private OrderFormView orderFormView;

    private TownshipAuthenticationContext townshipAuthenticationContext;

    private OrderEntity orderEntity;

    private Binder<OrderEntity> binder;

    private ListDataProvider<BillItem> billItemGridDataProvider;

    private GridListDataView<BillItem> billItemGridListDataView;

    private AtomicInteger gridBillItemsCounter = new AtomicInteger();


    public OrderFormPresenter(OrderEntityRepository orderEntityRepository) {
        this.orderEntityRepository = orderEntityRepository;
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

        getGridBillItems().forEach(billItem -> savingOrder.addItem(
                billItem.getProductEntity(),
                billItem.getAmount()
        ));


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

}
