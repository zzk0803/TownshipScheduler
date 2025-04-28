package zzk.townshipscheduler.ui.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Value;
import zzk.townshipscheduler.backend.scheduling.model.FactoryComputedDataTimePair;
import zzk.townshipscheduler.backend.scheduling.model.FactoryProcessSequence;
import zzk.townshipscheduler.backend.scheduling.model.IGameArrangeObject;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayer;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingExecutionMode;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingWorkCalendar;



@Value
@AllArgsConstructor
public class SchedulingProducingArrangementVO {

    int id;

    String uuid;

    String product;

    String arrangeFactory;

    String arrangeFactoryId;

    String producingDuration;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime arrangeDateTime;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime gameProducingDateTime;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime gameCompletedDateTime;

    public SchedulingProducingArrangementVO(SchedulingProducingArrangement schedulingProducingArrangement) {
        this.id = schedulingProducingArrangement.getId();
        this.uuid = schedulingProducingArrangement.getUuid();
        this.product=schedulingProducingArrangement.getSchedulingProduct().getName();
        this.arrangeFactory=schedulingProducingArrangement.getPlanningFactoryInstance().getFactoryReadableIdentifier().toString();
        this.arrangeFactoryId=schedulingProducingArrangement.getPlanningFactoryInstance().getId().toString();
        this.producingDuration=schedulingProducingArrangement.getProducingDuration().toString();
        this.arrangeDateTime=schedulingProducingArrangement.getArrangeDateTime();
        this.gameProducingDateTime=schedulingProducingArrangement.getProducingDateTime();
        this.gameCompletedDateTime=schedulingProducingArrangement.getCompletedDateTime();
    }

}
