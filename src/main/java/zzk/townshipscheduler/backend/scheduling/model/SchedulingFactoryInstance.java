package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.NavigableSet;
import java.util.TreeSet;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
//@PlanningEntity
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

//    @ShadowVariable(supplierName = "supplierNameFactorySlotToFinishedLocalDateTimeMap")
//    private TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> factorySlotToFinishedLocalDateTimeMap
//            = new TreeMap<>();

//    @ShadowSources({"schedulingFactoryInstanceDateTimeSlots[].tailArrangementCompletedDateTime"})
//    private TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> supplierNameFactorySlotToFinishedLocalDateTimeMap() {
//        if (!weatherFactoryProducingTypeIsQueue()) {
//            return null;
//        }
//
//        return this.schedulingFactoryInstanceDateTimeSlots.stream()
//                .collect(
//                        TreeMap::new,
//                        (treeMap, factoryInstanceDateTimeSlot) -> {
//                            treeMap.put(
//                                    factoryInstanceDateTimeSlot,
//                                    factoryInstanceDateTimeSlot.getApproximatedCompletedDateTime()
//                            );
//                        },
//                        TreeMap::putAll
//                );
//    }

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
                ", producingLength=" + producingQueue +
                ", reapWindowSize=" + reapWindowSize +
                '}';
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo()
                .typeEqual(that.getSchedulingFactoryInfo());
    }

}
