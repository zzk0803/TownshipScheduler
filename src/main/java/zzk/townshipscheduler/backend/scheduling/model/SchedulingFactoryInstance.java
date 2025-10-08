package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
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
import java.util.List;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Gatherer;

@Log4j2
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

    public static final String PLANNING_PRODUCING_ARRANGEMENTS = "planningProducingArrangements";

    private static final Gatherer<SchedulingProducingArrangement, Void, Pair<SchedulingProducingArrangement, FactoryComputedDateTimePair>>
            SLOT_GATHERER
            = Gatherer.of(
            () -> null,
            (_, schedulingProducingArrangement, downstream) -> {
                LocalDateTime start = schedulingProducingArrangement.getArrangeDateTime();
                downstream.push(new Pair<>(
                        schedulingProducingArrangement, new FactoryComputedDateTimePair(
                        start,
                        start.plus(schedulingProducingArrangement.getProducingDuration())
                )
                ));
                return true;
            },
            Gatherer.defaultCombiner(),
            Gatherer.defaultFinisher()
    );

    private static final Gatherer<SchedulingProducingArrangement, FormerCompletedDateTimeRef, Pair<SchedulingProducingArrangement, FactoryComputedDateTimePair>>
            QUEUE_GATHERER
            = createGatherer(
            (formerCompletedDateTime, factoryProcessSequence) -> {
                LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
                return (formerCompletedDateTime == null)
                        ? arrangeDateTime
                        : formerCompletedDateTime.isAfter(arrangeDateTime)
                                ? formerCompletedDateTime
                                : arrangeDateTime;
            }
    );

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    @JsonIgnore
    @EqualsAndHashCode.Include
    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    @Setter(AccessLevel.PRIVATE)
    private FactoryReadableIdentifier factoryReadableIdentifier;

    @JsonIgnore
    @PlanningListVariable(valueRangeProviderRefs = TownshipSchedulingProblem.VALUE_RANGE_FOR_ARRANGEMENTS)
    private List<SchedulingProducingArrangement> planningProducingArrangements = new ArrayList<>();

    @ShadowVariable(supplierName = "arrangementToComputedPairMapSupplier")
    private TreeMap<SchedulingProducingArrangement, FactoryComputedDateTimePair> arrangementToComputedPairMap
            = new TreeMap<>(SchedulingProducingArrangement.COMPARATOR);

    private static Gatherer<SchedulingProducingArrangement, FormerCompletedDateTimeRef, Pair<SchedulingProducingArrangement, FactoryComputedDateTimePair>>
    createGatherer(BiFunction<LocalDateTime, SchedulingProducingArrangement, LocalDateTime> startTimeComputer) {
        return Gatherer.ofSequential(
                FormerCompletedDateTimeRef::new,
                (formerCompletedDateTimeRef, schedulingProducingArrangement, downstream) -> {
                    LocalDateTime start = startTimeComputer.apply(
                            formerCompletedDateTimeRef.value,
                            schedulingProducingArrangement
                    );
                    LocalDateTime end = start.plus(schedulingProducingArrangement.getProducingDuration());
                    return downstream.push(new Pair<>(
                                    schedulingProducingArrangement,
                                    new FactoryComputedDateTimePair(
                                            start,
                                            formerCompletedDateTimeRef.value = end
                                    )
                            )
                    );
                }
        );
    }

    @ShadowSources(
            value = {
                    "planningProducingArrangements",
                    "planningProducingArrangements[].arrangeDateTime",
                    "planningProducingArrangements[].indexInFactoryArrangements"
            }
    )
    private TreeMap<SchedulingProducingArrangement, FactoryComputedDateTimePair> arrangementToComputedPairMapSupplier() {
        log.info("planningProducingArrangements={}", this.planningProducingArrangements);
        TreeMap<SchedulingProducingArrangement, FactoryComputedDateTimePair> result
                = this.planningProducingArrangements.stream()
                .filter(schedulingProducingArrangement -> schedulingProducingArrangement.getArrangeDateTime() != null && schedulingProducingArrangement.getIndexInFactoryArrangements() != null)
                .sorted(SchedulingProducingArrangement::compareTo)
                .gather(weatherFactoryProducingTypeIsQueue() ? QUEUE_GATHERER : SLOT_GATHERER)
                .collect(
                        TreeMap::new,
                        (treeMap, arrangementComputedPair) -> {
                            treeMap.put(arrangementComputedPair.getValue0(), arrangementComputedPair.getValue1());
                        },
                        TreeMap::putAll
                );
        log.info("arrangementToComputedPairMap={}", result);
        return result;
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

    private static final class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
