package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
    public boolean remainProducingLengthHadIllegal() {
        var resourceChanges
                = producingArrangementFactorySlotList.stream()
                .map(SchedulingFactorySlotProducingArrangement::calcConsequence)
                .flatMap(Collection::stream)
                .filter(consequence -> consequence.getResource().getRoot() == this)
                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryProducingLength)
                .sorted()
                .map(ActionConsequence::getResourceChange)
                .toList();

        int remain = getProducingLength();
        for (ActionConsequence.SchedulingResourceChange resourceChange : resourceChanges) {
            remain = resourceChange.apply(remain);
            if (remain < 0) {
                return true;
            }
        }
        return false;
    }

}
