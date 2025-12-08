package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Gatherer;

@Log4j2
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

    //<editor-fold desc="SLOT_GATHERER">
    private static final Gatherer<FactoryProcessSequence, Void, FactoryComputedDateTimeTuple>
            SLOT_GATHERER
            = Gatherer.of(
            () -> null,
            (_, factoryProcessSequence, downstream) -> {
                SchedulingDateTimeSlot schedulingDateTimeSlot = factoryProcessSequence.getPlanningDateTimeSlot();
                if (schedulingDateTimeSlot == null) {
                    return true;
                }

                LocalDateTime start = schedulingDateTimeSlot.getStart();
                return downstream.push(
                        new FactoryComputedDateTimeTuple(
                                factoryProcessSequence,
                                start,
                                start.plus(factoryProcessSequence.getProducingDuration())
                        )
                ) && !downstream.isRejecting();
            },
            Gatherer.defaultCombiner(),
            Gatherer.defaultFinisher()
    );
    //</editor-fold>

    //<editor-fold desc="QUEUE_GATHERER">
    private static final Gatherer<FactoryProcessSequence, FormerCompletedDateTimeRef, FactoryComputedDateTimeTuple>
            QUEUE_GATHERER
            = Gatherer.ofSequential(
            FormerCompletedDateTimeRef::new,
            (formerCompletedDateTimeRef, factoryProcessSequence, downstream) -> {

                LocalDateTime arrangeDateTime = factoryProcessSequence.getPlanningDateTimeSlot()
                        .getStart();
                LocalDateTime start = (formerCompletedDateTimeRef.value == null)
                        ? arrangeDateTime
                        : formerCompletedDateTimeRef.value.isAfter(arrangeDateTime)
                                ? formerCompletedDateTimeRef.value
                                : arrangeDateTime;
                LocalDateTime end = start.plus(factoryProcessSequence.getProducingDuration());
                return downstream.push(
                        new FactoryComputedDateTimeTuple(
                                factoryProcessSequence,
                                start,
                                formerCompletedDateTimeRef.value = end
                        )
                ) && !downstream.isRejecting();
            }
    );
    //</editor-fold>

    private Integer id;

    @PlanningId
    @EqualsAndHashCode.Include
    private String uuid;

    @JsonIgnore
    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    @Setter(AccessLevel.PRIVATE)
    private FactoryReadableIdentifier factoryReadableIdentifier;

    @JsonIgnore
    @InverseRelationShadowVariable(sourceVariableName = SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE)
    private List<SchedulingProducingArrangement> planningProducingArrangements = new ArrayList<>();

    @ShadowVariable(supplierName = "supplierForShadowComputedPairMap")
    private LinkedHashMap<FactoryProcessSequence, FactoryComputedDateTimeTuple> shadowComputedPairMap
            = new LinkedHashMap<>();

    @ShadowSources(
            value = {
                    "planningProducingArrangements",
                    "planningProducingArrangements[].factoryProcessSequence"
            }
    )
    public LinkedHashMap<FactoryProcessSequence, FactoryComputedDateTimeTuple> supplierForShadowComputedPairMap() {
        return this.planningProducingArrangements.stream()
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .map(SchedulingProducingArrangement::getFactoryProcessSequence)
                .sorted(FactoryProcessSequence.COMPARATOR)
                .gather(weatherFactoryProducingTypeIsQueue()
                        ? QUEUE_GATHERER
                        : SLOT_GATHERER)
                .collect(
                        LinkedHashMap::new,
                        (treeMap, tuple) -> treeMap.put(
                                tuple.factoryProcessSequence(),
                                tuple
                        ),
                        LinkedHashMap::putAll
                );
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo()
                .weatherFactoryProducingTypeIsQueue();
    }

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(new FactoryReadableIdentifier(
                getCategoryName(),
                getSeqNum()
        ));
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
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

    public FactoryComputedDateTimeTuple query(SchedulingProducingArrangement schedulingProducingArrangement) {
        return query(schedulingProducingArrangement.getFactoryProcessSequence());
    }

    public FactoryComputedDateTimeTuple query(FactoryProcessSequence factoryProcessSequence) {
        return this.shadowComputedPairMap.get(factoryProcessSequence);
    }

    private static final class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
