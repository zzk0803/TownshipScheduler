package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
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

    public static final int BENDABLE_SCORE_HARD_SIZE = 2;

    public static final int BENDABLE_SCORE_SOFT_SIZE = 3;

    public static final int HARD_BROKEN_PRODUCE_PREREQUISITE = 0;

    public static final int HARD_BROKEN_FACTORY_ABILITY = 1;

    public static final int SOFT_BATTER = 0;

    public static final int SOFT_BROKEN_DEADLINE = 1;

    public static final int SOFT_TOLERANCE = 2;

    public static final String VALUE_RANGE_FOR_DATE_TIME_SLOT = "valueRangeForDateTimeSlot";

    private String uuid;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductList;

    @PlanningEntityCollectionProperty
    private List<SchedulingFactoryInfo> schedulingFactoryInfoList;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderList;

    @PlanningEntityCollectionProperty
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

    @ProblemFactCollectionProperty
    public List<SchedulingArrangementHierarchies> toSchedulingArrangementHierarchies() {
        return this.schedulingProducingArrangementList.stream()
                .flatMap(
                        schedulingProducingArrangement -> schedulingProducingArrangement.toDeepPrerequisiteHierarchies()
                                .stream()
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }

}
