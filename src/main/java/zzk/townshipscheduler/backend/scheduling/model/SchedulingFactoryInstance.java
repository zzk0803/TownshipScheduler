package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

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
    private int producingLength;

    @ToString.Include
    private int reapWindowSize;

    @Setter(AccessLevel.PRIVATE)
    @ToString.Include
    private FactoryReadableIdentifier factoryReadableIdentifier;

    private NavigableSet<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlots = new TreeSet<>();

    @ShadowVariable(supplierName = "supplierNameFactorySlotToInfluencedByMap")
    private TreeMap<SchedulingFactoryInstanceDateTimeSlot, SchedulingFactoryInstanceDateTimeSlot> factorySlotToInfluencedByMap
            = new TreeMap<>();

    @ShadowSources(value = {"schedulingFactoryInstanceDateTimeSlots[].influencedFactorySlotSet"})
    private TreeMap<SchedulingFactoryInstanceDateTimeSlot, SchedulingFactoryInstanceDateTimeSlot> factorySlotToInfluencedByMap() {
        if (!weatherFactoryProducingTypeIsQueue()) {
            return null;
        }

        TreeMap<SchedulingFactoryInstanceDateTimeSlot, SchedulingFactoryInstanceDateTimeSlot>
                factorySlotToInfluencedByMap = new TreeMap<>();
        for (SchedulingFactoryInstanceDateTimeSlot factoryInstanceDateTimeSlot : schedulingFactoryInstanceDateTimeSlots) {
            Set<SchedulingFactoryInstanceDateTimeSlot> influencedFactorySlotSet = factoryInstanceDateTimeSlot.getInfluencedFactorySlotSet();
            if (Objects.nonNull(influencedFactorySlotSet) && !influencedFactorySlotSet.isEmpty()) {
                for (SchedulingFactoryInstanceDateTimeSlot influencedFactorySlot : influencedFactorySlotSet) {
                    factorySlotToInfluencedByMap.put(influencedFactorySlot, factoryInstanceDateTimeSlot);
                }
            }
        }

        return factorySlotToInfluencedByMap;
    }

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(
                new FactoryReadableIdentifier(
                        getCategoryName(),
                        getSeqNum()
                )
        );
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo()
                .weatherFactoryProducingTypeIsQueue();
    }

    @Override
    public String toString() {
        return "SchedulingFactoryInstance{" +
                "readableIdentifier='" + factoryReadableIdentifier + '\'' +
                ", producingLength=" + producingLength +
                ", reapWindowSize=" + reapWindowSize +
                '}';
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo()
                .typeEqual(that.getSchedulingFactoryInfo());
    }

}
