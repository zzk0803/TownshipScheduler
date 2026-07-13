package zzk.townshipscheduler.ui.pojo.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.ui.pojo.scheduling.reactive.ReactiveSchedulingProducingArrangementViewModel;

import java.time.LocalDateTime;


public record LitSchedulingProducingArrangementVO(
        int id,
        String uuid,
        String order,
        String product,
        String orderProduct,
        int orderProductArrangementId,
        boolean boolDirectToOrder,
        String factoryReadableIdentifier,
        String producingDuration,
        @JsonInclude @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime arrangeDateTime,
        @JsonInclude @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime producingDateTime,
        @JsonInclude @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime completedDateTime
) {

    public LitSchedulingProducingArrangementVO update(
            String factoryReadableIdentifier,
            LocalDateTime arrangeDateTime,
            LocalDateTime producingDateTime,
            LocalDateTime completedDateTime
    ) {
        return new LitSchedulingProducingArrangementVO(
                id,
                uuid,
                order,
                product,
                orderProduct,
                orderProductArrangementId,
                boolDirectToOrder,
                factoryReadableIdentifier,
                producingDuration,
                arrangeDateTime,
                producingDateTime,
                completedDateTime
        );
    }

    public static LitSchedulingProducingArrangementVO of(ReactiveSchedulingProducingArrangementViewModel schedulingProducingArrangement, boolean signalMode) {
        if (signalMode) {
            SchedulingFactoryInstanceViewModel schedulingFactoryInstanceViewModel = schedulingProducingArrangement.assignedFactoryInstance().get();
            return new LitSchedulingProducingArrangementVO(
                    schedulingProducingArrangement.arrangementViewModelId().id(),
                    schedulingProducingArrangement.arrangementViewModelId()
                            .uuid(),
                    String.valueOf(schedulingProducingArrangement.order()
                            .id()),
                    schedulingProducingArrangement.product()
                            .name(),
                    schedulingProducingArrangement.orderProduct()
                            .name(),
                    schedulingProducingArrangement.orderProductArrangementId()
                            .id(),
                    schedulingProducingArrangement.boolDirectToOrder(),
                    schedulingFactoryInstanceViewModel != null
                            ? schedulingFactoryInstanceViewModel.factoryReadableIdentifier()
                            : "N/A",
                    schedulingProducingArrangement.producingDuration()
                            .toString(),
                    schedulingProducingArrangement.arrangeDateTime().get(),
                    schedulingProducingArrangement.producingDateTime().get(),
                    schedulingProducingArrangement.completedDateTime().get()
            );
        }else {
            SchedulingFactoryInstanceViewModel schedulingFactoryInstanceViewModel = schedulingProducingArrangement.assignedFactoryInstance().peek();
            return new LitSchedulingProducingArrangementVO(
                    schedulingProducingArrangement.arrangementViewModelId().id(),
                    schedulingProducingArrangement.arrangementViewModelId()
                            .uuid(),
                    String.valueOf(schedulingProducingArrangement.order()
                            .id()),
                    schedulingProducingArrangement.product()
                            .name(),
                    schedulingProducingArrangement.orderProduct()
                            .name(),
                    schedulingProducingArrangement.orderProductArrangementId()
                            .id(),
                    schedulingProducingArrangement.boolDirectToOrder(),
                    schedulingFactoryInstanceViewModel != null
                            ? schedulingFactoryInstanceViewModel.factoryReadableIdentifier()
                            : "N/A",
                    schedulingProducingArrangement.producingDuration()
                            .toString(),
                    schedulingProducingArrangement.arrangeDateTime().peek(),
                    schedulingProducingArrangement.producingDateTime().peek(),
                    schedulingProducingArrangement.completedDateTime().peek()
            );
        }
    }

        public static LitSchedulingProducingArrangementVO of(
            SchedulingProducingArrangementViewModel schedulingProducingArrangement
    ) {
        var id = schedulingProducingArrangement.arrangementViewModelId()
                .id();
        var uuid = schedulingProducingArrangement.arrangementViewModelId()
                .uuid();
        var order = String.valueOf(schedulingProducingArrangement.order()
                .id());
        var product = schedulingProducingArrangement.product()
                .name();
        var orderProduct = schedulingProducingArrangement.orderProduct()
                .name();
        var orderProductArrangementId = schedulingProducingArrangement.orderProductArrangementId()
                .id();
        var boolDirectToOrder = schedulingProducingArrangement.boolDirectToOrder();
        SchedulingFactoryInstanceViewModel assignedFactoryInstance = schedulingProducingArrangement.assignedFactoryInstance();
        var factoryReadableIdentifier = assignedFactoryInstance != null
                                         ? assignedFactoryInstance.factoryReadableIdentifier()
                                         : null;
        var producingDuration = schedulingProducingArrangement.producingDuration()
                .toString();
        var arrangeDateTime = schedulingProducingArrangement.arrangeDateTime();
        var producingDateTime = schedulingProducingArrangement.producingDateTime();
        var completedDateTime = schedulingProducingArrangement.completedDateTime();
            return new LitSchedulingProducingArrangementVO(
                    id,
                    uuid,
                    order,
                    product,
                    orderProduct,
                    orderProductArrangementId,
                    boolDirectToOrder,
                    factoryReadableIdentifier,
                    producingDuration,
                    arrangeDateTime,
                    producingDateTime,
                    completedDateTime
            );
    }

    public static LitSchedulingProducingArrangementVO of(
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        SchedulingFactoryInstance planningFactoryInstance = schedulingProducingArrangement.getPlanningFactoryInstance();
        var id = schedulingProducingArrangement.getId();
        var uuid = schedulingProducingArrangement.getUuid()
                .toString();
        var order = String.valueOf(schedulingProducingArrangement.getSchedulingOrder()
                .getId());
        var product = schedulingProducingArrangement.getSchedulingProduct()
                .getName();
        var orderProduct = schedulingProducingArrangement.getSchedulingOrderProduct()
                .getName();
        var orderProductArrangementId = schedulingProducingArrangement.getSupportOrderProducingArrangement()
                .getId();
        var boolDirectToOrder = schedulingProducingArrangement.boolOrderDirect();
        var factoryReadableIdentifier = planningFactoryInstance != null
                                         ? planningFactoryInstance.getFactoryReadableIdentifier()
                                                 .toString()
                                         : null;
        var producingDuration = schedulingProducingArrangement.getProducingDuration()
                .toString();
        var arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
        var producingDateTime = schedulingProducingArrangement.getProducingDateTime();
        var completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
        return new LitSchedulingProducingArrangementVO(
                id,
                uuid,
                order,
                product,
                orderProduct,
                orderProductArrangementId,
                boolDirectToOrder,
                factoryReadableIdentifier,
                producingDuration,
                arrangeDateTime,
                producingDateTime,
                completedDateTime
        );
    }

}
