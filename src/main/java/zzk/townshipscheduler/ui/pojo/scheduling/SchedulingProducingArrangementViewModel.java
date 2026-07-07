package zzk.townshipscheduler.ui.pojo.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for {@link SchedulingProducingArrangement}
 */
public record SchedulingProducingArrangementViewModel(
        SchedulingProducingArrangementViewModelId arrangementViewModelId,
        SchedulingProductViewModel product,
        SchedulingProductViewModel orderProduct,
        SchedulingOrderViewModel order,
        SchedulingProducingArrangementViewModelId orderProductArrangementId,
        boolean boolDirectToOrder,
        List<SchedulingProducingArrangementViewModelId> prerequisiteProducingArrangements,
        Duration producingDuration,
        Duration staticDeepPrerequisiteProducingDuration,
        Duration staticDeepProducingDuration,
        SchedulingFactoryInstanceViewModel assignedFactoryInstance,
        @JsonInclude @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime arrangeDateTime,
        @JsonInclude @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime producingDateTime,
        @JsonInclude @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime completedDateTime
) implements Serializable {

    public boolean boolChild(SchedulingProducingArrangementViewModel arrangement) {
        return prerequisiteProducingArrangements.contains(arrangement.arrangementViewModelId);
    }

    public boolean boolParent(SchedulingProducingArrangementViewModel arrangement) {
        return arrangement.prerequisiteProducingArrangements.contains(this.arrangementViewModelId);
    }

    public SchedulingProducingArrangementViewModel update(
            SchedulingFactoryInstanceViewModel assignedFactoryInstance,
            LocalDateTime arrangeDateTime,
            LocalDateTime producingDateTime,
            LocalDateTime completedDateTime
    ) {
        return new SchedulingProducingArrangementViewModel(
                this.arrangementViewModelId,
                this.product,
                this.orderProduct,
                this.order,
                this.orderProductArrangementId,
                this.boolDirectToOrder,
                this.prerequisiteProducingArrangements,
                this.producingDuration,
                this.staticDeepPrerequisiteProducingDuration,
                this.staticDeepProducingDuration,
                assignedFactoryInstance,
                arrangeDateTime,
                producingDateTime,
                completedDateTime
        );
    }

    public record SchedulingProducingArrangementViewModelId(
            int id,
            String uuid
    ) implements Serializable {

        public static SchedulingProducingArrangementViewModelId of(
                int id,
                String uuid
        ) {
            return new SchedulingProducingArrangementViewModelId(
                    id,
                    uuid
            );
        }
    }

}
