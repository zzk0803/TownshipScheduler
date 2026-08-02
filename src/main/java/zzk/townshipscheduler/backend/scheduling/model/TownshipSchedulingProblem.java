package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.NavigableSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PlanningSolution
public class TownshipSchedulingProblem implements Serializable {

    public static final String VALUE_RANGE_FOR_DATE_TIME_SLOT_DELAY = "valueRangeForDateTimeSlotDelay";

    @Serial
    private static final long serialVersionUID = 719813705385100065L;

    public static final int ONE_DAY_MINUTES = 3600;

    private String uuid;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductList;

    @ProblemFactCollectionProperty
    private List<SchedulingProducingExecutionMode> schedulingProducingExecutionModeList;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInfo> schedulingFactoryInfoList;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderList;

    @PlanningEntityCollectionProperty
    private List<SchedulingFactoryInstance> schedulingFactoryInstanceList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    private NavigableSet<SchedulingDateTimeSlot> schedulingDateTimeSlots;

    @PlanningEntityCollectionProperty
    private NavigableSet<SchedulingProducingArrangement> schedulingProducingArrangements;

    @ProblemFactProperty
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @PlanningEntityProperty
    private SchedulingPlayer schedulingPlayer;

    @PlanningScore
    private transient HardMediumSoftBigDecimalScore score;

    private String scoreString;

    private DateTimeSlotSize dateTimeSlotSize;

    private SolverStatus solverStatus;

    public TownshipSchedulingProblem(
            String uuid,
            List<SchedulingProduct> schedulingProductList,
            List<SchedulingProducingExecutionMode> schedulingProducingExecutionModeList,
            List<SchedulingFactoryInfo> schedulingFactoryInfoList,
            List<SchedulingOrder> schedulingOrderList,
            List<SchedulingFactoryInstance> schedulingFactoryInstanceList,
            NavigableSet<SchedulingDateTimeSlot> schedulingDateTimeSlots,
            NavigableSet<SchedulingProducingArrangement> schedulingProducingArrangements,
            SchedulingWorkCalendar schedulingWorkCalendar,
            DateTimeSlotSize dateTimeSlotSize,
            SchedulingPlayer schedulingPlayer,
            HardMediumSoftBigDecimalScore score,
            SolverStatus solverStatus
    ) {
        this.uuid = uuid;
        this.schedulingProductList = schedulingProductList;
        this.schedulingProducingExecutionModeList = schedulingProducingExecutionModeList;
        this.schedulingFactoryInfoList = schedulingFactoryInfoList;
        this.schedulingOrderList = schedulingOrderList;
        this.schedulingFactoryInstanceList = schedulingFactoryInstanceList;
        this.schedulingDateTimeSlots = schedulingDateTimeSlots;
        this.schedulingProducingArrangements = schedulingProducingArrangements;
        this.schedulingWorkCalendar = schedulingWorkCalendar;
        this.schedulingPlayer = schedulingPlayer;
        this.score = score;
        this.dateTimeSlotSize = dateTimeSlotSize;
        this.solverStatus = solverStatus;
    }

    public static TownshipSchedulingProblemBuilder builder() {
        return new TownshipSchedulingProblemBuilder();
    }

}
