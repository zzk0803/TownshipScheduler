package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
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
import java.util.stream.Collectors;
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

    @DeepPlanningClone
    private TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> deltaComputeFactorySequenceToComputedPairMap
            = new TreeMap<>();

//    @ShadowVariable(supplierName = "supplierForShadowFullComputeFactorySequenceToComputedPairMap")
//    private TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> shadowFullComputeFactorySequenceToComputedPairMap
//            = new TreeMap<>();

    @ShadowVariable(supplierName = "supplierForShadowDeltaComputeFactorySequenceToComputedPairMap")
    private TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> shadowDeltaComputeFactorySequenceToComputedPairMap
            = new TreeMap<>();

//    @ShadowSources(value = {"planningProducingArrangements", "planningProducingArrangements[].factoryProcessSequence"})
//    public TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> supplierForShadowFullComputeFactorySequenceToComputedPairMap() {
//        return fullCompute();
//    }

    private TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> fullCompute() {
        return this.planningProducingArrangements.stream()
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .sorted(SchedulingProducingArrangement.COMPARATOR)
                .map(SchedulingProducingArrangement::getFactoryProcessSequence)
                .gather(weatherFactoryProducingTypeIsQueue() ? QUEUE_GATHERER : SLOT_GATHERER)
                .collect(
                        TreeMap::new,
                        (treeMap, pair) -> treeMap.put(
                                pair.getValue0(),
                                pair.getValue1()
                        ),
                        TreeMap::putAll
                );
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue();
    }

    @ShadowSources(
            value = {
                    "planningProducingArrangements[].producingDateTime",
                    "planningProducingArrangements[].completedDateTime"
            }
    )
    public TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> supplierForShadowDeltaComputeFactorySequenceToComputedPairMap() {
        return this.deltaComputeFactorySequenceToComputedPairMap;
    }

    public FactoryComputedDateTimePair deltaCompute(FactoryProcessSequence arg) {
        List<FactoryProcessSequence> toDel = deltaComputeFactorySequenceToComputedPairMap.keySet().stream()
                .filter(exist
                        -> exist.getArrangementId().equals(arg.getArrangementId())
                           && (
                                   !exist.getArrangeDateTime().equals(arg.getArrangeDateTime())
                                   || !exist.getSchedulingFactoryInstanceReadableIdentifier()
                                           .equals(arg.getSchedulingFactoryInstanceReadableIdentifier())
                           )
                ).collect(Collectors.toCollection(ArrayList::new));

        if (!toDel.isEmpty()) {
            for (FactoryProcessSequence sequence : toDel) {
                this.removeFactoryProcessSequence(sequence);
            }
        }

        addFactoryProcessSequence(arg);

        return deltaComputeFactorySequenceToComputedPairMap.get(arg);
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

    public void addFactoryProcessSequence(FactoryProcessSequence factoryProcessSequence) {

        if (!weatherFactoryProducingTypeIsQueue()) {
            this.deltaComputeFactorySequenceToComputedPairMap.put(
                    factoryProcessSequence,
                    new FactoryComputedDateTimePair(
                            factoryProcessSequence.getArrangeDateTime(),
                            factoryProcessSequence.getArrangeDateTime()
                                    .plus(factoryProcessSequence.getProducingDuration())
                    )
            );
            return;
        }

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prefixProducingPairEntry =
                this.deltaComputeFactorySequenceToComputedPairMap.lowerEntry(factoryProcessSequence);

        LocalDateTime producingDateTime = calcProducingDateTime(
                factoryProcessSequence,
                prefixProducingPairEntry
        );
        LocalDateTime completedDateTime = calcCompletedDateTime(factoryProcessSequence, producingDateTime);
        this.deltaComputeFactorySequenceToComputedPairMap.put(
                factoryProcessSequence,
                new FactoryComputedDateTimePair(producingDateTime, completedDateTime)
        );

        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap
                = this.deltaComputeFactorySequenceToComputedPairMap.tailMap(
                factoryProcessSequence,
                false
        );
        cascade(tailOfProcessSequencePairMap);
    }

    private void cascade(NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap) {
        if (tailOfProcessSequencePairMap.isEmpty()) {
            return;
        }

        List<FactoryProcessSequence> taiKeyList = new ArrayList<>(tailOfProcessSequencePairMap.keySet());

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prefixEntry
                = this.deltaComputeFactorySequenceToComputedPairMap.lowerEntry(taiKeyList.get(0));

        LocalDateTime prefixMaxCompletedDateTime
                = (prefixEntry == null)
                ? null
                : prefixEntry.getValue().completedDateTime();

        for (FactoryProcessSequence factoryProcessSequence : taiKeyList) {
            LocalDateTime producingDateTime = calcProducingDateTime(
                    factoryProcessSequence,
                    prefixMaxCompletedDateTime
            );
            LocalDateTime completedDateTime = calcCompletedDateTime(
                    factoryProcessSequence,
                    producingDateTime
            );
            this.deltaComputeFactorySequenceToComputedPairMap.put(
                    factoryProcessSequence,
                    new FactoryComputedDateTimePair(producingDateTime, completedDateTime)
            );
            prefixMaxCompletedDateTime = completedDateTime;
        }
    }

    private LocalDateTime calcProducingDateTime(
            FactoryProcessSequence factoryProcessSequence,
            LocalDateTime previousCompletedDateTime
    ) {
        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompletedDateTime == null || arrangeDateTime.isAfter(previousCompletedDateTime))
                ? arrangeDateTime
                : previousCompletedDateTime;
    }

    private LocalDateTime calcCompletedDateTime(
            FactoryProcessSequence factoryProcessSequence,
            LocalDateTime producingDateTime
    ) {
        return producingDateTime.plus(factoryProcessSequence.getProducingDuration());
    }

    private LocalDateTime calcProducingDateTime(
            FactoryProcessSequence factoryProcessSequence,
            Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> previousEntry
    ) {
        LocalDateTime previousCompleted = (previousEntry == null)
                ? null
                : previousEntry.getValue().completedDateTime();

        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompleted == null || arrangeDateTime.isAfter(previousCompleted))
                ? arrangeDateTime
                : previousCompleted;
    }

    public void removeFactoryProcessSequence(FactoryProcessSequence factoryProcessSequence) {

        if (this.deltaComputeFactorySequenceToComputedPairMap.remove(factoryProcessSequence) == null) {
            return;
        }

        if (!weatherFactoryProducingTypeIsQueue()) {
            return;
        }

        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tail =
                this.deltaComputeFactorySequenceToComputedPairMap.tailMap(factoryProcessSequence, false);
        cascade(tail);
    }

    private LocalDateTime calcProducingDateTime(
            FactoryProcessSequence factoryProcessSequence,
            FactoryComputedDateTimePair previousComputedPair
    ) {
        LocalDateTime previousCompleted = (previousComputedPair == null)
                ? null
                : previousComputedPair.completedDateTime();

        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompleted == null || arrangeDateTime.isAfter(previousCompleted))
                ? arrangeDateTime
                : previousCompleted;
    }

    private static final class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
