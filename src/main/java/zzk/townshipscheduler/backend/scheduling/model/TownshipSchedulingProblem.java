package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

@Slf4j
@Data
@NoArgsConstructor
@PlanningSolution
public class TownshipSchedulingProblem implements Serializable {

    @Serial
    private static final long serialVersionUID = 719813705385100065L;

    private String uuid;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductList;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInfo> schedulingFactoryInfoList;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderList;

    @PlanningEntityCollectionProperty
    private List<SchedulingFactoryInstance> schedulingFactoryInstanceList;

    @ProblemFactCollectionProperty
    private TreeSet<SchedulingDateTimeSlot> schedulingDateTimeSlots;

    @PlanningEntityCollectionProperty
    private List<SchedulingProducingArrangement> schedulingProducingArrangementList;

    @ProblemFactProperty
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @ProblemFactProperty
    private SchedulingPlayer schedulingPlayer;

    @PlanningScore
    private HardMediumSoftBigDecimalScore score;

    private DateTimeSlotSize dateTimeSlotSize;

    private SolverStatus solverStatus;

    public TownshipSchedulingProblem(
            String uuid,
            List<SchedulingProduct> schedulingProductList,
            List<SchedulingFactoryInfo> schedulingFactoryInfoList,
            List<SchedulingOrder> schedulingOrderList,
            List<SchedulingFactoryInstance> schedulingFactoryInstanceList,
            TreeSet<SchedulingDateTimeSlot> schedulingDateTimeSlots,
            List<SchedulingProducingArrangement> schedulingProducingArrangementList,
            SchedulingWorkCalendar schedulingWorkCalendar,
            DateTimeSlotSize dateTimeSlotSize,
            SchedulingPlayer schedulingPlayer,
            HardMediumSoftBigDecimalScore score,
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

    public List<SchedulingProducingArrangement> valueRangeForSchedulingProducingArrangement(SchedulingFactoryInstance schedulingFactoryInstance) {
        return getSchedulingProducingArrangementList().stream()
                .filter(schedulingProducingArrangement -> schedulingProducingArrangement.getRequiredFactoryInfo()
                        .equals(schedulingFactoryInstance.getSchedulingFactoryInfo()))
                .toList();
    }

    public SchedulingDateTimeSlot getDateTimeSlotWithMinDateTimeAndDelayAmount(
            LocalDateTime floorLocalDateTime,
            Integer delaySlot
    ) {
        Objects.requireNonNull(floorLocalDateTime);

        int delayMinute = (
                delaySlot == null
                        ? 0
                        : delaySlot
        ) * getDateTimeSlotSize().getMinute();

        return SchedulingDateTimeSlot.ceilingDateTimeFromValueRange(
                this.schedulingDateTimeSlots,
                floorLocalDateTime.plusMinutes(delayMinute)
        );
    }

}
