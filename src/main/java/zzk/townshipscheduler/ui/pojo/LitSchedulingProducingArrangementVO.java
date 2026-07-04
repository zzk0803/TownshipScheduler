package zzk.townshipscheduler.ui.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Value;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.time.LocalDateTime;


@Value
@AllArgsConstructor
public class LitSchedulingProducingArrangementVO {

    int id;

    String uuid;

    String order;

    String product;

    String orderProduct;

    int orderProductArrangementId;

    boolean boolDirectToOrder;

    String factoryReadableIdentifier;

    String producingDuration;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime arrangeDateTime;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime producingDateTime;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime completedDateTime;

    public LitSchedulingProducingArrangementVO(
            SchedulingProducingArrangementViewModel schedulingProducingArrangement
    ) {
        this.id = schedulingProducingArrangement.arrangementViewModelId()
                .id();
        this.uuid = schedulingProducingArrangement.arrangementViewModelId()
                .uuid();
        this.order = String.valueOf(schedulingProducingArrangement.order()
                .id());
        this.product = schedulingProducingArrangement.product()
                .name();
        this.orderProduct = schedulingProducingArrangement.orderProduct()
                .name();
        this.orderProductArrangementId = schedulingProducingArrangement.orderProductArrangementId()
                .id();
        this.boolDirectToOrder = schedulingProducingArrangement.boolDirectToOrder();
        SchedulingFactoryInstanceViewModel assignedFactoryInstance = schedulingProducingArrangement.assignedFactoryInstance();
        this.factoryReadableIdentifier = assignedFactoryInstance != null
                                         ? assignedFactoryInstance.factoryReadableIdentifier()
                                         : null;
        this.producingDuration = schedulingProducingArrangement.producingDuration()
                .toString();
        this.arrangeDateTime = schedulingProducingArrangement.arrangeDateTime();
        this.producingDateTime = schedulingProducingArrangement.producingDateTime();
        this.completedDateTime = schedulingProducingArrangement.completedDateTime();
    }

    public LitSchedulingProducingArrangementVO(
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        SchedulingFactoryInstance planningFactoryInstance = schedulingProducingArrangement.getPlanningFactoryInstance();
        this.id = schedulingProducingArrangement.getId();
        this.uuid = schedulingProducingArrangement.getUuid()
                .toString();
        this.order = String.valueOf(schedulingProducingArrangement.getSchedulingOrder()
                .getId());
        this.product = schedulingProducingArrangement.getSchedulingProduct()
                .getName();
        this.orderProduct = schedulingProducingArrangement.getSchedulingOrderProduct()
                .getName();
        this.orderProductArrangementId = schedulingProducingArrangement.getSupportOrderProducingArrangement()
                .getId();
        this.boolDirectToOrder = schedulingProducingArrangement.boolOrderDirect();
        this.factoryReadableIdentifier = planningFactoryInstance != null
                                         ? planningFactoryInstance.getFactoryReadableIdentifier()
                                                 .toString()
                                         : null;
        this.producingDuration = schedulingProducingArrangement.getProducingDuration()
                .toString();
        this.arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
        this.producingDateTime = schedulingProducingArrangement.getProducingDateTime();
        this.completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
    }

}
