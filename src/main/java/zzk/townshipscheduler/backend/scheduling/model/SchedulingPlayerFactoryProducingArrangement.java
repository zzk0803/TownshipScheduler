package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true,callSuper = true)
@PlanningEntity
public class SchedulingPlayerFactoryProducingArrangement
        extends AbstractPlayerProducingArrangement {

    public static final String PLANNING_FACTORY = "planningFactory";

    @PlanningVariable(
            valueRangeProviderRefs = TownshipSchedulingProblem.FACTORY_VALUE_RANGE
    )
    private SchedulingFactoryInstanceMultiple planningFactory;

    @ToString.Include
    private List<SchedulingFactoryInstanceMultiple> ValueRangeForPlanningFactory;

    public SchedulingPlayerFactoryProducingArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        super(targetActionObject, currentActionObject);
    }

    @Override
    public void activate(
            ActionIdRoller idRoller,
            SchedulingWorkTimeLimit workTimeLimit,
            SchedulingPlayer schedulingPlayer
    ) {
        super.activate(idRoller, workTimeLimit, schedulingPlayer);
        prepareValueRangeForSchedulingFactoryInstance();
    }

    public void prepareValueRangeForSchedulingFactoryInstance() {
        SchedulingProduct schedulingProduct = getSchedulingProduct();
        SchedulingFactoryInfo requireFactory = schedulingProduct.getRequireFactory();
        ValueRangeForPlanningFactory = new ArrayList<>(requireFactory.getFactoryInstances());
    }

    @Override
    public SchedulingFactoryInstanceMultiple getFactory() {
        return getPlanningFactory();
    }

    @ValueRangeProvider(id = TownshipSchedulingProblem.FACTORY_VALUE_RANGE)
    public List<SchedulingFactoryInstanceMultiple> valueRangeForSchedulingFactoryInstance() {
        return Collections.unmodifiableList(ValueRangeForPlanningFactory);
    }

}
