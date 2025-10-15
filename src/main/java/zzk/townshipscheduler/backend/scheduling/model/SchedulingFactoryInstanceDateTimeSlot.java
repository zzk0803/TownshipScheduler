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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@PlanningEntity
public class SchedulingFactoryInstanceDateTimeSlot implements Comparable<SchedulingFactoryInstanceDateTimeSlot> {

    public static final String PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS = "planningSchedulingProducingArrangements";

    public static final Comparator<SchedulingFactoryInstanceDateTimeSlot> COMPARATOR
            = Comparator.comparing(SchedulingFactoryInstanceDateTimeSlot::getDateTimeSlot)
            .thenComparingInt(SchedulingFactoryInstanceDateTimeSlot::getId);

    @PlanningId
    private int id;

    @ToString.Include
    @EqualsAndHashCode.Include
    private FactoryDateTimeReadableIdentifier factoryDateTimeReadableIdentifier;

    @EqualsAndHashCode.Include
    private SchedulingFactoryInstance factoryInstance;

    private SchedulingDateTimeSlot dateTimeSlot;

    private SchedulingFactoryInstanceDateTimeSlot previous;

    private SchedulingFactoryInstanceDateTimeSlot next;

    @PlanningListVariable(valueRangeProviderRefs = TownshipSchedulingProblem.VALUE_RANGE_FOR_PRODUCING_ARRANGEMENTS)
    private List<SchedulingProducingArrangement> planningSchedulingProducingArrangements = new ArrayList<>();

    @ShadowVariable(supplierName = "tailArrangementCompletedDateTimeSupplier")
    private LocalDateTime tailArrangementCompletedDateTime;

    public SchedulingFactoryInstanceDateTimeSlot(
            int id,
            SchedulingFactoryInstance schedulingFactoryInstance,
            SchedulingDateTimeSlot schedulingDateTimeSlot
    ) {
        this.id = id;
        this.factoryInstance = schedulingFactoryInstance;
        this.dateTimeSlot = schedulingDateTimeSlot;
        this.factoryDateTimeReadableIdentifier = new FactoryDateTimeReadableIdentifier(
                schedulingFactoryInstance,
                schedulingDateTimeSlot.getStart(),
                schedulingDateTimeSlot.getEnd()
        );
    }

    @ShadowSources({"planningSchedulingProducingArrangements"})
    private LocalDateTime tailArrangementCompletedDateTimeSupplier() {
        if (getPlanningSchedulingProducingArrangements().isEmpty()) {
            return this.getStart();
        }

        return this.planningSchedulingProducingArrangements.getLast().getCompletedDateTime();
    }

    @EqualsAndHashCode.Include
    public SchedulingFactoryInfo getSchedulingFactoryInfo() {
        return factoryInstance.getSchedulingFactoryInfo();
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

    public boolean boolInfluenceBy(SchedulingFactoryInstanceDateTimeSlot that) {
        return that.getTailArrangementCompletedDateTime().isAfter(this.getStart());
    }

    @EqualsAndHashCode.Include
    public LocalDateTime getStart() {
        return dateTimeSlot.getStart();
    }

    public boolean boolInfluenceTo(SchedulingFactoryInstanceDateTimeSlot that) {
        return this.getTailArrangementCompletedDateTime().isAfter(that.getStart());
    }

    @Override
    public int compareTo(@NotNull SchedulingFactoryInstanceDateTimeSlot that) {
        return COMPARATOR.compare(this, that)
                ;
    }

}
