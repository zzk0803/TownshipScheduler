package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Data
@PlanningSolution
@NoArgsConstructor
public class TownshipSchedulingProblem implements Serializable {

    @Serial
    private static final long serialVersionUID = -399118697021610459L;

    //    public static final String VALUE_RANGE_FOR_FACTORIES = "valueRangeForFactories";

    //    public static final String VALUE_RANGE_FOR_DATE_TIME_SLOT = "valueRangeForDateTimeSlot";

    private String uuid;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductList;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInfo> schedulingFactoryInfoList;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderList;

    @PlanningEntityCollectionProperty
//    @ValueRangeProvider(id = VALUE_RANGE_FOR_FACTORIES)
    private List<SchedulingFactoryInstance> schedulingFactoryInstanceList;

    @PlanningEntityCollectionProperty
//    @ValueRangeProvider(id = VALUE_RANGE_FOR_DATE_TIME_SLOT)
    private List<SchedulingDateTimeSlot> schedulingDateTimeSlots;

    @PlanningEntityCollectionProperty
    private List<SchedulingProducingArrangement> schedulingProducingArrangementList;

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
            List<SchedulingProducingArrangement> schedulingProducingArrangementList,
            SchedulingWorkCalendar schedulingWorkCalendar,
            DateTimeSlotSize dateTimeSlotSize,
            SchedulingPlayer schedulingPlayer,
            HardMediumSoftLongScore score,
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

    public List<SchedulingProducingArrangement> lookupProducingArrangements(
            SchedulingFactoryInstance schedulingFactoryInstance
    ) {
        return getSchedulingProducingArrangementList().stream()
                .filter(schedulingProducingArrangement -> schedulingProducingArrangement.getPlanningFactoryInstance() == schedulingFactoryInstance)
                .toList();
    }

    public List<SchedulingProducingArrangement> lookupProducingArrangements(
            Collection<FactoryProcessSequence> factoryProcessSequences
    ) {
        return getSchedulingProducingArrangementList().stream()
                .filter(schedulingProducingArrangement -> factoryProcessSequences.contains(schedulingProducingArrangement.getShadowFactoryProcessSequence()))
                .toList();
    }

    public Optional<SchedulingFactoryInstance> lookupFactoryInstance(
            FactoryReadableIdentifier factoryReadableIdentifier
    ) {
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

    public List<SchedulingFactoryInstance> valueRangeFactoryInstancesForArrangement(
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        SchedulingFactoryInfo requiredFactoryInfo = schedulingProducingArrangement.getRequiredFactoryInfo();
        return this.schedulingFactoryInstanceList.stream()
                .filter(schedulingFactoryInstance -> schedulingFactoryInstance.getSchedulingFactoryInfo()
                        .typeEqual(requiredFactoryInfo))
                .toList();
    }

    public List<SchedulingDateTimeSlot> valueRangeDateTimeSlotsForArrangement(
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        LocalDateTime startDateTime = getSchedulingWorkCalendar().getStartDateTime();
        Duration staticDeepPrerequisiteProducingDuration =
                schedulingProducingArrangement.getStaticDeepPrerequisiteProducingDuration();
        LocalDateTime atLeastArrangeDateTime = startDateTime.plus(staticDeepPrerequisiteProducingDuration);
        return this.schedulingDateTimeSlots.stream()
                .filter(schedulingDateTimeSlot -> !schedulingDateTimeSlot.getStart().isBefore(atLeastArrangeDateTime))
                .toList();
    }

}
