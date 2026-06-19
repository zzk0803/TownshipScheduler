package zzk.townshipscheduler.ui.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Value;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.time.LocalDateTime;
import java.util.Objects;


@Value
@AllArgsConstructor
public class SchedulingProducingArrangementVO {

    int id;

    String uuid;

    String order;

    String product;

    String orderProduct;

    int orderProductArrangementId;

    boolean boolDirectToOrder;

    String factoryReadableIdentifier;

    String producingDuration;

    int planningIndexInFactory;

    int planningDelaySlotNum;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime prerequisiteCompletedDateTime;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime arrangeDateTime;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime producingDateTime;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime completedDateTime;

    public SchedulingProducingArrangementVO(
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
        this.orderProductArrangementId = schedulingProducingArrangement.getSchedulingOrderProductArrangementId();
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
        this.planningIndexInFactory = Objects.nonNull(schedulingProducingArrangement.getIndexInFactory())
                ? schedulingProducingArrangement.getIndexInFactory()
                : -1;
        this.planningDelaySlotNum = Objects.nonNull(schedulingProducingArrangement.getPlanningDelaySlot())
                ? schedulingProducingArrangement.getPlanningDelaySlot()
                : -1;
        this.prerequisiteCompletedDateTime = schedulingProducingArrangement.getShadowPrerequisiteProducingArrangementsFinishedDateTime();
    }

}
