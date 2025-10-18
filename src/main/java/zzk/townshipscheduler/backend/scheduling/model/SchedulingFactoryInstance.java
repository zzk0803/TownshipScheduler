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
import org.javatuples.Pair;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Gatherer;

@Log4j2
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

    //<editor-fold desc="SLOT_GATHERER">
    private static final Gatherer<FactoryProcessSequence, Void, Pair<FactoryProcessSequence, FactoryComputedDateTimePair>>
            SLOT_GATHERER
            = Gatherer.of(
            () -> null,
            (_, factoryProcessSequence, downstream) -> {
                SchedulingDateTimeSlot schedulingDateTimeSlot = factoryProcessSequence.getPlanningDateTimeSlot();
                if (schedulingDateTimeSlot == null) {
                    return true;
                }

                LocalDateTime start = schedulingDateTimeSlot.getStart();
                return downstream.push(new Pair<>(
                        factoryProcessSequence,
                        new FactoryComputedDateTimePair(
                                start,
                                start.plus(factoryProcessSequence.getProducingDuration())
                        )
                )) && !downstream.isRejecting();
            },
            Gatherer.defaultCombiner(),
            Gatherer.defaultFinisher()
    );
    //</editor-fold>

    //<editor-fold desc="QUEUE_GATHERER">
    private static final Gatherer<FactoryProcessSequence, FormerCompletedDateTimeRef, Pair<FactoryProcessSequence, FactoryComputedDateTimePair>>
            QUEUE_GATHERER
            = Gatherer.ofSequential(
            FormerCompletedDateTimeRef::new,
            (formerCompletedDateTimeRef, factoryProcessSequence, downstream) -> {
                SchedulingDateTimeSlot schedulingDateTimeSlot = factoryProcessSequence.getPlanningDateTimeSlot();
                if (schedulingDateTimeSlot == null) {
                    return true;
                }

                LocalDateTime arrangeDateTime = schedulingDateTimeSlot.getStart();
                LocalDateTime start = (formerCompletedDateTimeRef.value == null)
                        ? arrangeDateTime
                        : formerCompletedDateTimeRef.value.isAfter(arrangeDateTime)
                                ? formerCompletedDateTimeRef.value
                                : arrangeDateTime;
                LocalDateTime end = start.plus(factoryProcessSequence.getProducingDuration());
                return downstream.push(
                        new Pair<>(
                                factoryProcessSequence,
                                new FactoryComputedDateTimePair(
                                        start,
                                        formerCompletedDateTimeRef.value = end
                                )
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
    private LinkedHashMap<FactoryProcessSequence, FactoryComputedDateTimePair> shadowComputedPairMap
            = new LinkedHashMap<>();

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue();
    }

    @ShadowSources(
            value = {
                    "planningProducingArrangements",
                    "planningProducingArrangements[].factoryProcessSequence"
            }
    )
    public LinkedHashMap<FactoryProcessSequence, FactoryComputedDateTimePair> supplierForShadowComputedPairMap() {
        return this.planningProducingArrangements.stream()
                .map(SchedulingProducingArrangement::getFactoryProcessSequence)
                .sorted(FactoryProcessSequence.COMPARATOR)
                .gather(weatherFactoryProducingTypeIsQueue() ? QUEUE_GATHERER : SLOT_GATHERER)
                .collect(
                        LinkedHashMap::new,
                        (treeMap, pair) -> treeMap.put(
                                pair.getValue0(),
                                pair.getValue1()
                        ),
                        LinkedHashMap::putAll
                );
    }

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(new FactoryReadableIdentifier(getCategoryName(), getSeqNum()));
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
        return this.getSchedulingFactoryInfo().typeEqual(that.getSchedulingFactoryInfo());
    }

    public FactoryComputedDateTimePair query(FactoryProcessSequence factoryProcessSequence) {
        return this.shadowComputedPairMap.get(factoryProcessSequence);
    }

    private static final class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
