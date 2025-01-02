package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SchedulingGameActionDummy extends SchedulingGameAction {

    public SchedulingGameActionDummy(Flag flag) {
        setFlag(flag);
    }

    @Override
    public String getHumanReadable() {
        return "Dummy Action";
    }

}
