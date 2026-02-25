package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
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

    public static final String RANGE_FOR_ARRANGEMENTS = "valueRangeForArrangements";

    public static final String PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS = "planningSchedulingProducingArrangements";

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

    @DeepPlanningClone
    private List<SchedulingFactoryInstanceDateTimeSlot> formerFactorySlotList = new ArrayList<>();

    @PlanningListVariable(valueRangeProviderRefs = RANGE_FOR_ARRANGEMENTS)
    private List<SchedulingProducingArrangement> planningSchedulingProducingArrangements = new ArrayList<>();

    @ShadowVariable(supplierName = "amendedFirstArrangementProducingDateTimeSupplier")
    private LocalDateTime amendedFirstArrangementProducingDateTime;

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

    @ValueRangeProvider(id = RANGE_FOR_ARRANGEMENTS)
    public List<SchedulingProducingArrangement> valueRangeForArrangements(
            TownshipSchedulingProblem townshipSchedulingProblem
    ) {
        return townshipSchedulingProblem.valueRangeForArrangements(this);
    }

    @ShadowSources(value = {"formerFactorySlotList[].planningSchedulingProducingArrangements"})
    public LocalDateTime amendedFirstArrangementProducingDateTimeSupplier() {
        if (getFactoryInstance().weatherFactoryProducingTypeIsQueue()) {
            LocalDateTime result = getFormerFactorySlotList().stream()
                    .flatMap(factoryInstanceDateTimeSlot -> factoryInstanceDateTimeSlot.getPlanningSchedulingProducingArrangements()
                            .stream()
                    )
                    .map(SchedulingProducingArrangement::getCompletedDateTime)
                    .filter(localDateTime -> localDateTime.isAfter(this.getStart()))
                    .max(Comparator.naturalOrder())
                    .orElse(this.getStart())
                    ;
            if (!result.equals(getStart())) {
                log.info("{} amend first arrangement producing datetime::{}", this.factoryDateTimeReadableIdentifier, result);
            }
            return result;
        } else {
            return getStart();
        }
    }

    @EqualsAndHashCode.Include
    public LocalDateTime getStart() {
        return dateTimeSlot.getStart();
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
        return factoryInstance.getProducingLength();
    }

    public int getReapWindowSize() {
        return factoryInstance.getReapWindowSize();
    }

//    public Optional<SchedulingFactoryInstanceDateTimeSlot> boolInfluenceBy(
//            Collection<SchedulingFactoryInstanceDateTimeSlot> those
//    ) {
//        if (those == null || those.isEmpty()) {
//            return Optional.empty();
//        }
//
//        return those.stream()
//                .filter(this::boolInfluenceBy)
//                .min(Comparator.naturalOrder());
//    }

//    public boolean boolInfluenceBy(SchedulingFactoryInstanceDateTimeSlot that) {
//        LocalDateTime tailArrangementCompletedDateTime = that.getFormerFactorySlotsCompletedDateTime();
//        if (tailArrangementCompletedDateTime == null) {
//            return false;
//        }
//        return tailArrangementCompletedDateTime.isAfter(this.getStart());
//    }

//    public Collection<SchedulingFactoryInstanceDateTimeSlot> boolInfluenceTo(
//            Collection<SchedulingFactoryInstanceDateTimeSlot> those
//    ) {
//        return those.stream()
//                .sorted(Comparator.naturalOrder())
//                .filter(this::boolInfluenceTo)
//                .collect(Collectors.toCollection(TreeSet::new));
//    }

//    public boolean boolInfluenceTo(SchedulingFactoryInstanceDateTimeSlot that) {
//        LocalDateTime thisTailArrangementDateTime = this.getFormerFactorySlotsCompletedDateTime();
//        LocalDateTime thatStart = that.getStart();
//        if (thisTailArrangementDateTime == null) {
//            return false;
//        }
//        return thisTailArrangementDateTime.isAfter(thatStart);
//    }

    @Override
    public int compareTo(@NotNull SchedulingFactoryInstanceDateTimeSlot that) {
        return SchedulingDateTimeSlot.DATE_TIME_SLOT_COMPARATOR.compare(
                this.dateTimeSlot,
                that.dateTimeSlot
        );
    }

}
