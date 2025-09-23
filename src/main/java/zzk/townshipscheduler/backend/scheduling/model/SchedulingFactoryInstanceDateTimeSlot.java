package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Log4j2
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

    @ShadowSources({"planningSchedulingProducingArrangements[].completedDateTime"})
    private LocalDateTime tailArrangementCompletedDateTimeSupplier() {
        if (getSchedulingFactoryInfo().getProducingStructureType() == ProducingStructureType.SLOT) {
            return null;
        }

        if (getPlanningSchedulingProducingArrangements().isEmpty()) {
            return null;
        }

        LocalDateTime completedDateTime = planningSchedulingProducingArrangements.getLast().getCompletedDateTime();
        log.info("***  factory={},tailArrangementCompletedDateTime={}",factoryInstance.getFactoryReadableIdentifier(),completedDateTime);
        return completedDateTime;
    }

    public SchedulingFactoryInstanceDateTimeSlot(
            int id,
            SchedulingFactoryInstance schedulingFactoryInstance,
            SchedulingDateTimeSlot schedulingDateTimeSlot
    ) {
        this.id = id;
        this.factoryInstance = schedulingFactoryInstance;
        this.dateTimeSlot = schedulingDateTimeSlot;
    }

    @EqualsAndHashCode.Include
    public LocalDateTime getStart() {
        return dateTimeSlot.getStart();
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

    @EqualsAndHashCode.Include
    public SchedulingFactoryInfo getSchedulingFactoryInfo() {
        return factoryInstance.getSchedulingFactoryInfo();
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
