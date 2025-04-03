package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.javatuples.Pair;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@PlanningEntity
public class SchedulingTypeSlotFactoryInstance extends BaseSchedulingFactoryInstance {

    @InverseRelationShadowVariable(sourceVariableName = SchedulingFactorySlotProducingArrangement.PLANNING_FACTORY)
    private List<SchedulingFactorySlotProducingArrangement> producingArrangementFactorySlotList = new ArrayList<>();

    @Override
    public Pair<Integer, Duration> remainProducingCapacityAndNextAvailable(SchedulingDateTimeSlot schedulingDateTimeSlot) {
        var filteredArrangeConsequences
                = producingArrangementFactorySlotList.stream()
                .map(SchedulingFactorySlotProducingArrangement::calcConsequence)
                .flatMap(Collection::stream)
                .filter(consequence -> consequence.getResource().getRoot() == this)
                .filter(consequence -> consequence.getResource() instanceof ArrangeConsequence.FactoryProducingLength)
                .sorted()
                .toList();

        Duration firstCapacityIncreaseDuration
                = filteredArrangeConsequences.stream()
                .filter(arrangeConsequence -> arrangeConsequence.getLocalDateTime().isAfter(schedulingDateTimeSlot.getStart()))
                .filter(arrangeConsequence -> arrangeConsequence.getResourceChange() instanceof ArrangeConsequence.Increase)
                .findFirst()
                .map(arrangeConsequence -> Duration.between(
                        schedulingDateTimeSlot.getStart(),
                        arrangeConsequence.getLocalDateTime()
                ))
                .orElse(null);

        int remain = filteredArrangeConsequences.stream()
                .filter(arrangeConsequence -> arrangeConsequence.getLocalDateTime()
                                                      .isBefore(schedulingDateTimeSlot.getStart())
                                              || arrangeConsequence.getLocalDateTime()
                                                      .isEqual(schedulingDateTimeSlot.getStart()))
                .reduce(
                        getProducingLength(),
                        (integer, arrangeConsequence) -> arrangeConsequence.getResourceChange().apply(integer),
                        Integer::sum
                );

        return Pair.with(remain, remain > 0 ? Duration.ZERO : firstCapacityIncreaseDuration);
    }

}
