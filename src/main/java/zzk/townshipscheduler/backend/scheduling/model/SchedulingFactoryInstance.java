package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SchedulingFactoryInstance extends BasePlanningChainSupportFactoryOrAction {

    @PlanningId
    private String instanceId;

    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    public void setSchedulingFactoryInfo(SchedulingFactoryInfo schedulingFactoryInfo) {
        this.schedulingFactoryInfo = schedulingFactoryInfo;
        setupFactoryInstanceId();
    }

    private void setupFactoryInstanceId() {
        this.instanceId = getSchedulingFactoryInfo().getCategoryName() + "#" + getSeqNum();

    }

}
