package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.javatuples.Pair;

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

    @JsonIgnore
    @InverseRelationShadowVariable(sourceVariableName = SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE)
    private List<SchedulingProducingArrangement> planningFactoryInstanceProducingArrangements = new ArrayList<>();

    @JsonIgnore
    @DeepPlanningClone
    private SortedSet<SchedulingDateTimeSlot.FactoryProcessSequence> shadowFactorySequenceSet
            = new ConcurrentSkipListSet<>(
            Comparator.comparing(SchedulingDateTimeSlot.FactoryProcessSequence::getArrangeDateTime)
                    .thenComparingInt(SchedulingDateTimeSlot.FactoryProcessSequence::getArrangementId)
    );

    public Pair<LocalDateTime, LocalDateTime> queryProducingAndCompletedPair(SchedulingProducingArrangement schedulingProducingArrangement) {
        if (schedulingProducingArrangement.weatherFactoryProducingTypeIsQueue()) {
            return prepareAndGet(
                    this.shadowFactorySequenceSet,
                    schedulingProducingArrangement.getShadowFactoryProcessSequence()
            );
        } else {
            LocalDateTime arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
            return new Pair<>(
                    arrangeDateTime,
                    arrangeDateTime != null
                            ? arrangeDateTime.plus(schedulingProducingArrangement.getProducingDuration())
                            : null
            );
        }
    }

    private Pair<LocalDateTime, LocalDateTime> prepareAndGet(
            SortedSet<SchedulingDateTimeSlot.FactoryProcessSequence> sortedSet,
            SchedulingDateTimeSlot.FactoryProcessSequence shadowFactoryProcessSequence
    ) {
        ConcurrentSkipListMap<SchedulingDateTimeSlot.FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> computingProducingCompletedMap
                = new ConcurrentSkipListMap<>(
                Comparator.comparing(SchedulingDateTimeSlot.FactoryProcessSequence::getArrangeDateTime)
                        .thenComparingInt(SchedulingDateTimeSlot.FactoryProcessSequence::getArrangementId)
        );

        for (SchedulingDateTimeSlot.FactoryProcessSequence current : sortedSet) {
            Duration producingDuration = current.getProducingDuration();
            LocalDateTime arrangeDateTime = current.getArrangeDateTime();

            LocalDateTime previousAlmostCompletedDateTime
                    = computingProducingCompletedMap.headMap(current, false)
                    .values().stream()
                    .map(Pair::getValue1)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            LocalDateTime producingDateTime;
            if (previousAlmostCompletedDateTime == null) {
                producingDateTime = arrangeDateTime;
            } else {
                if (arrangeDateTime.isAfter(previousAlmostCompletedDateTime)) {
                    producingDateTime = arrangeDateTime;
                } else if(arrangeDateTime.isBefore(previousAlmostCompletedDateTime)){
                    producingDateTime = previousAlmostCompletedDateTime;
                }else {
                    producingDateTime = arrangeDateTime;
                }
            }
            LocalDateTime completedDateTime = producingDateTime.plus(producingDuration);

            computingProducingCompletedMap.put(
                    current,
                    new Pair<>(producingDateTime, completedDateTime)
            );

        }

        return computingProducingCompletedMap.get(shadowFactoryProcessSequence);
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

}
