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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Gatherer;

@Log4j2
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

    private static final Gatherer<SchedulingProducingArrangement, Void, Pair<SchedulingProducingArrangement, FactoryComputedDateTimePair>>
            SLOT_GATHERER
            = Gatherer.of(
            () -> null,
            (_, schedulingProducingArrangement, downstream) -> {
                SchedulingDateTimeSlot schedulingDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
                if (schedulingDateTimeSlot == null) {
                    return true;
                }

                LocalDateTime start = schedulingDateTimeSlot.getStart();
                return downstream.push(new Pair<>(
                        schedulingProducingArrangement,
                        new FactoryComputedDateTimePair(
                                start,
                                start.plus(schedulingProducingArrangement.getProducingDuration())
                        )
                )) && !downstream.isRejecting();
            },
            Gatherer.defaultCombiner(),
            Gatherer.defaultFinisher()
    );

    private static final Gatherer<SchedulingProducingArrangement, FormerCompletedDateTimeRef, Pair<SchedulingProducingArrangement, FactoryComputedDateTimePair>>
            QUEUE_GATHERER
            = Gatherer.ofSequential(
            FormerCompletedDateTimeRef::new,
            (formerCompletedDateTimeRef, schedulingProducingArrangement, downstream) -> {
                SchedulingDateTimeSlot schedulingDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
                if (schedulingDateTimeSlot == null) {
                    return true;
                }

                LocalDateTime arrangeDateTime = schedulingDateTimeSlot.getStart();
                LocalDateTime start = (formerCompletedDateTimeRef.value == null)
                        ? arrangeDateTime
                        : formerCompletedDateTimeRef.value.isAfter(arrangeDateTime)
                                ? formerCompletedDateTimeRef.value
                                : arrangeDateTime;
                LocalDateTime end = start.plus(schedulingProducingArrangement.getProducingDuration());
                return downstream.push(
                        new Pair<>(
                                schedulingProducingArrangement,
                                new FactoryComputedDateTimePair(
                                        start,
                                        formerCompletedDateTimeRef.value = end
                                )
                        )
                ) && !downstream.isRejecting();
            }
    );

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

    @ShadowVariable(supplierName = "supplierForArrangementToComputedPairMap")
    private LinkedHashMap<SchedulingProducingArrangement, FactoryComputedDateTimePair> arrangementToComputedPairMap = new LinkedHashMap<>();

    @ShadowSources(
            {
                    "planningProducingArrangements",
                    "planningProducingArrangements[].planningDateTimeSlot"
            }
    )
    public LinkedHashMap<SchedulingProducingArrangement, FactoryComputedDateTimePair> supplierForArrangementToComputedPairMap() {
        return this.planningProducingArrangements.stream()
                .sorted(SchedulingProducingArrangement.COMPARATOR)
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

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue();
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

    public FactoryComputedDateTimePair queryComputedDateTimePair(SchedulingProducingArrangement schedulingProducingArrangement) {
        return this.arrangementToComputedPairMap.get(schedulingProducingArrangement);
    }

    private static final class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
