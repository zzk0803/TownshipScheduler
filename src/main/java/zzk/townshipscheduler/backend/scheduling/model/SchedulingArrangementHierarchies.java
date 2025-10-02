package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchedulingArrangementHierarchies {

    SchedulingProducingArrangement whole;

    SchedulingProducingArrangement partial;

}
