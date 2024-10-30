package zzk.townshipscheduler.backend.scheduling.model.entity;

import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.scheduling.model.WorkPlace;
import zzk.townshipscheduler.backend.scheduling.model.fact.SchedulingBarn;

import java.time.LocalDateTime;

public abstract class SchedulingMove {

    //@ShadowVariable(variableListenerClass = )
    @Getter
    protected SchedulingBarn barn;

    @Getter
    @Setter
    @InverseRelationShadowVariable(sourceVariableName = "previousMove")
    protected SchedulingMove nextMove;

    //@ShadowVariable()
    private LocalDateTime finishDateTime;

    protected abstract WorkPlace getWorkPlace();

    protected abstract boolean boolWorkPlaceSupport();

    protected abstract void doWorkInPlace();

    public void moveTransaction() {
        if (boolWorkPlaceSupport()) {
            doWorkInPlace();
        }
    }

}
