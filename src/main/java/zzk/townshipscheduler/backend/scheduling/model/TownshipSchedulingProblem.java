package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

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

    private String uuid;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductList;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInfo> schedulingFactoryInfoList;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderList;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInstance> schedulingFactoryInstanceList;

    @ProblemFactCollectionProperty
    private List<SchedulingDateTimeSlot> schedulingDateTimeSlots;

    @PlanningEntityCollectionProperty
    private List<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlotList;

    @PlanningEntityCollectionProperty
    private List<SchedulingProducingArrangement> schedulingProducingArrangementList;

    @PlanningEntityProperty
    private SchedulingArrangementsGlobalState schedulingArrangementsGlobalState;

    @ProblemFactProperty
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @ProblemFactProperty
    private SchedulingPlayer schedulingPlayer;

    @PlanningScore
    private HardMediumSoftLongScore score;

    private DateTimeSlotSize dateTimeSlotSize;

    private SolverStatus solverStatus;

    public TownshipSchedulingProblem(
            String uuid,
            List<SchedulingProduct> schedulingProductList,
            List<SchedulingFactoryInfo> schedulingFactoryInfoList,
            List<SchedulingOrder> schedulingOrderList,
            List<SchedulingFactoryInstance> schedulingFactoryInstanceList,
            List<SchedulingDateTimeSlot> schedulingDateTimeSlots,
            List<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlotList,
            List<SchedulingProducingArrangement> schedulingProducingArrangementList,
            SchedulingArrangementsGlobalState schedulingArrangementsGlobalState,
            SchedulingWorkCalendar schedulingWorkCalendar,
            SchedulingPlayer schedulingPlayer,
            HardMediumSoftLongScore score,
            DateTimeSlotSize dateTimeSlotSize,
            SolverStatus solverStatus
    ) {
        this.uuid = uuid;
        this.schedulingProductList = schedulingProductList;
        this.schedulingFactoryInfoList = schedulingFactoryInfoList;
        this.schedulingOrderList = schedulingOrderList;
        this.schedulingFactoryInstanceList = schedulingFactoryInstanceList;
        this.schedulingDateTimeSlots = schedulingDateTimeSlots;
        this.schedulingFactoryInstanceDateTimeSlotList = schedulingFactoryInstanceDateTimeSlotList;
        this.schedulingProducingArrangementList = schedulingProducingArrangementList;
        this.schedulingArrangementsGlobalState = schedulingArrangementsGlobalState;
        this.schedulingWorkCalendar = schedulingWorkCalendar;
        this.schedulingPlayer = schedulingPlayer;
        this.score = score;
        this.dateTimeSlotSize = dateTimeSlotSize;
        this.solverStatus = solverStatus;
    }

    public static TownshipSchedulingProblemBuilder builder() {
        return new TownshipSchedulingProblemBuilder();
    }

    public List<SchedulingArrangementOrFactorySlot> valueRangeForArrangements(
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        SchedulingFactoryInfo schedulingFactoryInfo = schedulingProducingArrangement.getRequiredFactoryInfo();

        return Stream.concat(
                        this.schedulingProducingArrangementList.stream()
                                .filter(
                                        arrangement -> arrangement.getRequiredFactoryInfo().typeEqual(schedulingFactoryInfo)
                                ),
                        this.schedulingFactoryInstanceDateTimeSlotList.stream()
                                .filter(
                                        factoryInstanceDateTimeSlot -> factoryInstanceDateTimeSlot.getSchedulingFactoryInfo()
                                                .typeEqual(schedulingFactoryInfo)
                                )
                )
                .toList();
    }

}
