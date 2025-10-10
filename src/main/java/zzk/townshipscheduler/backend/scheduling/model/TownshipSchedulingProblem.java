package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@PlanningSolution
@NoArgsConstructor
public class TownshipSchedulingProblem {

    public static final int BENDABLE_SCORE_HARD_SIZE = 3;

    public static final int BENDABLE_SCORE_SOFT_SIZE = 2;

    public static final int HARD_BROKEN_FACTORY_ABILITY = 0;

    public static final int HARD_BROKEN_PRODUCE_PREREQUISITE = 1;

    public static final int HARD_BROKEN_DEADLINE = 2;

    public static final int SOFT_TOLERANCE = 0;

    public static final int SOFT_BATTER = 1;

    public static final String VALUE_RANGE_FOR_FACTORIES = "valueRangeForFactories";

    public static final String VALUE_RANGE_FOR_DATE_TIME_SLOT = "valueRangeForDateTimeSlot";

    private String uuid;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductList;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInfo> schedulingFactoryInfoList;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = VALUE_RANGE_FOR_FACTORIES)
    private List<SchedulingFactoryInstance> schedulingFactoryInstanceList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = VALUE_RANGE_FOR_DATE_TIME_SLOT)
    private List<SchedulingDateTimeSlot> schedulingDateTimeSlots;

    @PlanningEntityCollectionProperty
    private List<SchedulingProducingArrangement> schedulingProducingArrangementList;

    @ProblemFactProperty
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @ProblemFactProperty
    private SchedulingPlayer schedulingPlayer;

    @PlanningScore(
            bendableHardLevelsSize = BENDABLE_SCORE_HARD_SIZE,
            bendableSoftLevelsSize = BENDABLE_SCORE_SOFT_SIZE
    )
    private BendableLongScore score;

    private DateTimeSlotSize dateTimeSlotSize;

    private SolverStatus solverStatus;

    public TownshipSchedulingProblem(
            String uuid,
            List<SchedulingProduct> schedulingProductList,
            List<SchedulingFactoryInfo> schedulingFactoryInfoList,
            List<SchedulingOrder> schedulingOrderList,
            List<SchedulingFactoryInstance> schedulingFactoryInstanceList,
            List<SchedulingDateTimeSlot> schedulingDateTimeSlots,
            List<SchedulingProducingArrangement> schedulingProducingArrangementList,
            SchedulingWorkCalendar schedulingWorkCalendar,
            SchedulingPlayer schedulingPlayer,
            BendableLongScore score,
            DateTimeSlotSize dateTimeSlotSize,
            SolverStatus solverStatus
    ) {
        this.uuid = uuid;
        this.schedulingProductList = schedulingProductList;
        this.schedulingFactoryInfoList = schedulingFactoryInfoList;
        this.schedulingOrderList = schedulingOrderList;
        this.schedulingFactoryInstanceList = schedulingFactoryInstanceList;
        this.schedulingDateTimeSlots = schedulingDateTimeSlots;
        this.schedulingProducingArrangementList = schedulingProducingArrangementList;
        this.schedulingWorkCalendar = schedulingWorkCalendar;
        this.schedulingPlayer = schedulingPlayer;
        this.score = score;
        this.dateTimeSlotSize = dateTimeSlotSize;
        this.solverStatus = solverStatus;
    }

    public static TownshipSchedulingProblemBuilder builder() {
        return new TownshipSchedulingProblemBuilder();
    }

    public List<SchedulingProducingArrangement> lookupProducingArrangements(SchedulingFactoryInstance schedulingFactoryInstance) {
        return getSchedulingProducingArrangementList().stream()
                .filter(schedulingProducingArrangement -> schedulingProducingArrangement.getPlanningFactoryInstance() == schedulingFactoryInstance)
                .toList();
    }

    public Optional<SchedulingFactoryInstance> lookupFactoryInstance(FactoryReadableIdentifier factoryReadableIdentifier) {
        return getSchedulingFactoryInstanceList().stream()
                .filter(schedulingFactoryInstance -> schedulingFactoryInstance.getFactoryReadableIdentifier()
                        .equals(factoryReadableIdentifier))
                .findFirst();
    }

    public Optional<SchedulingFactoryInstance> lookupFactoryInstance(FactoryProcessSequence factoryProcessSequence) {
        return getSchedulingFactoryInstanceList().stream()
                .filter(schedulingFactoryInstance -> schedulingFactoryInstance.getFactoryReadableIdentifier()
                        .equals(factoryProcessSequence.getSchedulingFactoryInstanceReadableIdentifier()))
                .findFirst();
    }

    @ProblemFactCollectionProperty
    public List<SchedulingArrangementHierarchies> toSchedulingArrangementHierarchies() {
        return this.schedulingProducingArrangementList.stream()
                .flatMap(
                        schedulingProducingArrangement -> schedulingProducingArrangement.toPrerequisiteHierarchies()
                                .stream()
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }

}
