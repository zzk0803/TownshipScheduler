package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    @JsonIgnore
    @EqualsAndHashCode.Include
    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int parallelProducing = 1;

    @ToString.Include
    private int producingQueue;

    @ToString.Include
    private int reapWindowSize;

    @Setter(AccessLevel.PRIVATE)
    @ToString.Include
    private FactoryReadableIdentifier factoryReadableIdentifier;

    private NavigableSet<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlots = new TreeSet<>();

    @ShadowVariable(supplierName = "factorySlotToFirstArrangementProducingDateTimeMapSupplier")
    private TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> factorySlotToFirstArrangementProducingDateTimeMap
            = new TreeMap<>();

    @ShadowSources({"schedulingFactoryInstanceDateTimeSlotList[].tailArrangementCompletedDateTime"})
    private TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> factorySlotToFirstArrangementProducingDateTimeMapSupplier() {
        TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> result = new TreeMap<>();
        for (SchedulingFactoryInstanceDateTimeSlot current : schedulingFactoryInstanceDateTimeSlots) {
            Set<SchedulingFactoryInstanceDateTimeSlot> headSetOfCurrent
                    = schedulingFactoryInstanceDateTimeSlots.headSet(current, false);
            Optional<SchedulingFactoryInstanceDateTimeSlot> findInfluenceBy
                    = current.boolInfluenceBy(headSetOfCurrent);
            result.put(
                    current,
                    findInfluenceBy.map(SchedulingFactoryInstanceDateTimeSlot::getTailArrangementCompletedDateTime)
                            .orElse(current.getStart())
            );
        }
        return result;
    }

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(
                new FactoryReadableIdentifier(getCategoryName(), getSeqNum())
        );
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue();
    }

    @Override
    public String toString() {
        return "SchedulingFactoryInstance{" +
               "readableIdentifier='" + factoryReadableIdentifier + '\'' +
               ", producingLength=" + producingQueue +
               ", reapWindowSize=" + reapWindowSize +
               '}';
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo().typeEqual(that.getSchedulingFactoryInfo());
    }

}
