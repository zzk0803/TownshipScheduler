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
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    @JsonIgnore
    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    private String readableIdentifier;

    @JsonIgnore
    @InverseRelationShadowVariable(sourceVariableName = SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE)
    private List<SchedulingProducingArrangement> planningFactoryInstanceProducingArrangements = new ArrayList<>();

    public boolean addFactoryProcessSequence(FactoryProcessSequence factoryProcessSequence) {
        return shadowFactorySequenceSet.add(factoryProcessSequence);
    }

    public boolean removeFactoryProcessSequence(Object o) {
        return shadowFactorySequenceSet.remove(o);
    }

    @JsonIgnore
    @DeepPlanningClone
    private SortedSet<FactoryProcessSequence> shadowFactorySequenceSet
            = new ConcurrentSkipListSet<>(
            Comparator.comparing(FactoryProcessSequence::getArrangeDateTime)
                    .thenComparingInt(FactoryProcessSequence::getArrangementId)
    );

    public Pair<LocalDateTime, LocalDateTime> queryProducingAndCompletedPair(SchedulingProducingArrangement schedulingProducingArrangement) {
        if (schedulingProducingArrangement.weatherFactoryProducingTypeIsQueue()) {
            FactoryProcessSequence factoryProcessSequence = schedulingProducingArrangement.getShadowFactoryProcessSequence();
            SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> processSequenceDateTimePairMap
                    = prepareProducingAndCompletedMap();
            return processSequenceDateTimePairMap.get(factoryProcessSequence);
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

    public SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> prepareProducingAndCompletedMap() {
        SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> computingProducingCompletedMap
                = new ConcurrentSkipListMap<>(
                Comparator.comparing(FactoryProcessSequence::getArrangeDateTime)
                        .thenComparingInt(FactoryProcessSequence::getArrangementId)
        );

        for (FactoryProcessSequence current : this.shadowFactorySequenceSet) {
            Duration producingDuration = current.getProducingDuration();
            LocalDateTime arrangeDateTime = current.getArrangeDateTime();

            LocalDateTime previousAlmostCompletedDateTime
                    = computingProducingCompletedMap.headMap(current)
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
                } else if (arrangeDateTime.isBefore(previousAlmostCompletedDateTime)) {
                    producingDateTime = previousAlmostCompletedDateTime;
                } else {
                    producingDateTime = arrangeDateTime;
                }
            }
            LocalDateTime completedDateTime = producingDateTime.plus(producingDuration);

            computingProducingCompletedMap.put(
                    current,
                    new Pair<>(producingDateTime, completedDateTime)
            );

        }

        return computingProducingCompletedMap;
    }

    @Override
    public String toString() {
        return "SchedulingFactoryInstance{" +
               "readableIdentifier='" + readableIdentifier + '\'' +
               ", producingLength=" + producingLength +
               ", reapWindowSize=" + reapWindowSize +
               '}';
    }

    @JsonProperty
    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo().typeEqual(that.getSchedulingFactoryInfo());
    }

}
