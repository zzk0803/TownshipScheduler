package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.*;
import zzk.townshipscheduler.backend.scheduling.model.utility.ProducingArrangementComputedDateTimeVariableListener;

import java.time.Duration;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true)
//@PlanningEntity
public class SchedulingPlayerProducingArrangement
        extends AbstractPlayerProducingArrangement {

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private SchedulingFactoryInstanceSingle factoryInstance;

    public SchedulingPlayerProducingArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject,
            SchedulingFactoryInstanceSingle factoryInstance
    ) {
        super(targetActionObject, currentActionObject);
        this.factoryInstance = factoryInstance;
    }

    @Override
    public SchedulingFactoryInstanceSingle getFactory() {
        return this.getFactoryInstance();
    }

}
