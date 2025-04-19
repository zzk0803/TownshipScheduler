package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

    @PlanningId
    @EqualsAndHashCode.Include
    protected Integer id;

    @JsonIgnore
    protected SchedulingFactoryInfo schedulingFactoryInfo;

    protected int seqNum;

    protected int producingLength;

    protected int reapWindowSize;

    @InverseRelationShadowVariable(sourceVariableName = SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE)
    private List<SchedulingProducingArrangement> planningSchedulingProducingArrangements = new ArrayList<>();

    @DeepPlanningClone
    private SortedSet<SchedulingDateTimeSlot.FactoryProcessSequence> shadowFactorySequenceSet
            = new ConcurrentSkipListSet<>(
            Comparator.comparing(SchedulingDateTimeSlot.FactoryProcessSequence::getArrangeDateTime)
                    .thenComparingInt(SchedulingDateTimeSlot.FactoryProcessSequence::getArrangementId)
    );

    public LocalDateTime queryProducingDateTime(SchedulingProducingArrangement schedulingProducingArrangement) {
        return prepareAndGet(
                this.shadowFactorySequenceSet,
                schedulingProducingArrangement.getShadowFactoryProcessSequence()
        );
    }

    private static LocalDateTime prepareAndGet(
            SortedSet<SchedulingDateTimeSlot.FactoryProcessSequence> sortedSet,
            SchedulingDateTimeSlot.FactoryProcessSequence shadowFactoryProcessSequence
    ) {
        ConcurrentSkipListMap<SchedulingDateTimeSlot.FactoryProcessSequence, DateTimeRange> computingProducingCompletedMap
                = new ConcurrentSkipListMap<>(
                Comparator.comparing(SchedulingDateTimeSlot.FactoryProcessSequence::getArrangeDateTime)
                        .thenComparingInt(SchedulingDateTimeSlot.FactoryProcessSequence::getArrangementId)
        );

        for (
                SchedulingDateTimeSlot.FactoryProcessSequence currentFactoryProcessSequence
                : sortedSet
        ) {
            Duration producingDuration = currentFactoryProcessSequence.getProducingDuration();
            LocalDateTime arrangeDateTime = currentFactoryProcessSequence.getArrangeDateTime();

//            SchedulingDateTimeSlot.FactoryProcessSequence floorOfCurrent
//                    = computingProducingCompletedMap.floorKey(currentFactoryProcessSequence);

            LocalDateTime previousAlmostCompletedDateTime
                    = computingProducingCompletedMap.headMap(currentFactoryProcessSequence, false)
                    .values().stream()
                    .map(DateTimeRange::endDateTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

//            DateTimeRange previousDateTimeRange
//                    = floorOfCurrent != null
//                    ? computingProducingCompletedMap.get(floorOfCurrent)
//                    : null;
//            LocalDateTime previousCompletedDateTime
//                    = previousDateTimeRange != null
//                    ? previousDateTimeRange.endDateTime
//                    : null;

            LocalDateTime producingDateTime
                    = previousAlmostCompletedDateTime == null
                    ? arrangeDateTime
                    : arrangeDateTime.isAfter(previousAlmostCompletedDateTime)
                            ? arrangeDateTime
                            : previousAlmostCompletedDateTime;
            LocalDateTime completedDateTime = producingDateTime.plus(producingDuration);

            computingProducingCompletedMap.put(
                    currentFactoryProcessSequence,
                    new DateTimeRange(producingDateTime, completedDateTime)
            );

        }

        DateTimeRange timeRange = computingProducingCompletedMap.get(shadowFactoryProcessSequence);
        return timeRange != null ? timeRange.startDateTime : null;
    }

    @Override
    public String toString() {
        return this.schedulingFactoryInfo.getCategoryName() + "#" + this.getSeqNum() + ",size=" + this.getProducingLength();
    }

    public String getReadableIdentifier() {
        return this.schedulingFactoryInfo.getCategoryName() + "#" + this.getSeqNum();
    }

    @JsonProperty
    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo().typeEqual(that.getSchedulingFactoryInfo());
    }

    public record DateTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {

        public Duration getDuration() {
            return Duration.between(startDateTime, endDateTime);
        }

    }

}
