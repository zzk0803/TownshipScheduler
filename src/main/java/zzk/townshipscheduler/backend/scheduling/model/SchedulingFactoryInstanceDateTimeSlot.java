package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@PlanningEntity
public class SchedulingFactoryInstanceDateTimeSlot implements Comparable<SchedulingFactoryInstanceDateTimeSlot> {

    public static final String PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS = "planningSchedulingProducingArrangements";

    @PlanningId
    @EqualsAndHashCode.Include
    private int id;

    @ToString.Include
    @EqualsAndHashCode.Include
    private SchedulingFactoryInstance factoryInstance;

    @ToString.Include
    private SchedulingDateTimeSlot dateTimeSlot;

    private SchedulingFactoryInstanceDateTimeSlot previous;

    private SchedulingFactoryInstanceDateTimeSlot next;

    @ToString.Include
    @PlanningListVariable(valueRangeProviderRefs = TownshipSchedulingProblem.VALUE_RANGE_FOR_PRODUCING_ARRANGEMENTS)
    private List<SchedulingProducingArrangement> planningSchedulingProducingArrangements = new ArrayList<>();

    @ShadowVariable(supplierName = "tailArrangementCompletedDateTimeSupplier")
    private LocalDateTime tailArrangementCompletedDateTime;

    @ShadowVariable(supplierName = "firstArrangementProducingDateTimeSupplier")
    private LocalDateTime firstArrangementProducingDateTime;

    public SchedulingFactoryInstanceDateTimeSlot(
            int id,
            SchedulingFactoryInstance schedulingFactoryInstance,
            SchedulingDateTimeSlot schedulingDateTimeSlot
    ) {
        this.id = id;
        this.factoryInstance = schedulingFactoryInstance;
        this.dateTimeSlot = schedulingDateTimeSlot;
    }

    @ShadowSources({"planningSchedulingProducingArrangements"})
    private LocalDateTime tailArrangementCompletedDateTimeSupplier() {
        if (getSchedulingFactoryInfo().getProducingStructureType() == ProducingStructureType.SLOT) {
            return null;
        }

        if (getPlanningSchedulingProducingArrangements().isEmpty()) {
            return null;
        }

        var completedDateTime = getStart().plus(planningSchedulingProducingArrangements.stream()
                .map(SchedulingProducingArrangement::getProducingDuration)
                .reduce(Duration.ZERO, Duration::plus));

        return completedDateTime;
    }

    @EqualsAndHashCode.Include
    public LocalDateTime getStart() {
        return dateTimeSlot.getStart();
    }

    @EqualsAndHashCode.Include
    public SchedulingFactoryInfo getSchedulingFactoryInfo() {
        return factoryInstance.getSchedulingFactoryInfo();
    }

    @ShadowSources({"previous.tailArrangementCompletedDateTime", "tailArrangementCompletedDateTime"})
    public LocalDateTime firstArrangementProducingDateTimeSupplier() {
        LocalDateTime dateTimeSlotStart = this.dateTimeSlot.getStart();

        if (!weatherFactoryProducingTypeIsQueue()) {
            return dateTimeSlotStart;
        }

        LocalDateTime firstPreviousExceed = findFirstFormerExceed(dateTimeSlotStart);

        if (firstPreviousExceed == null) {
            return dateTimeSlotStart;
        }

        return firstPreviousExceed.isAfter(dateTimeSlotStart)
                ? firstPreviousExceed
                : dateTimeSlotStart;
    }

    private LocalDateTime findFirstFormerExceed(LocalDateTime targetDateTime) {
        if (this.previous == null) {
            return null;
        }

        LocalDateTime previousStart = this.previous.getStart();
        LocalDateTime previousTailArrangementCompletedDateTime = this.previous.getTailArrangementCompletedDateTime();

        if (Objects.nonNull(previousTailArrangementCompletedDateTime) && previousTailArrangementCompletedDateTime.isAfter(
                targetDateTime)) {
            return previousTailArrangementCompletedDateTime;
        }

        return this.previous.findFirstFormerExceed(previousStart);
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return factoryInstance.weatherFactoryProducingTypeIsQueue();
    }

    @EqualsAndHashCode.Include
    public LocalDateTime getEnd() {
        return dateTimeSlot.getEnd();
    }

    public int getDurationInMinute() {
        return dateTimeSlot.getDurationInMinute();
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return factoryInstance.typeEqual(that);
    }

    public String getCategoryName() {
        return factoryInstance.getCategoryName();
    }

    public int getSeqNum() {
        return factoryInstance.getSeqNum();
    }

    public int getProducingLength() {
        return factoryInstance.getProducingQueue();
    }

    public int getReapWindowSize() {
        return factoryInstance.getReapWindowSize();
    }

    public FactoryReadableIdentifier getFactoryReadableIdentifier() {
        return factoryInstance.getFactoryReadableIdentifier();
    }

    @Override
    public int compareTo(@NotNull SchedulingFactoryInstanceDateTimeSlot that) {
        return SchedulingDateTimeSlot.DATE_TIME_SLOT_COMPARATOR.compare(this.dateTimeSlot, that.dateTimeSlot);
    }

}
