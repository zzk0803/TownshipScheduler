package zzk.townshipscheduler.ui.pojo.scheduling;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInfo;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link SchedulingFactoryInfo}
 */
public record SchedulingFactoryInfoViewModel(
        long id,
        String categoryName,
        int level,
        String producingStructureType,
        List<SchedulingProductViewModel> productList,
        int defaultInstanceAmount,
        int defaultProducingCapacity,
        int defaultReapWindowCapacity,
        int maxProducingCapacity,
        int maxReapWindowCapacity,
        int maxInstanceAmount
) implements Serializable {

}
