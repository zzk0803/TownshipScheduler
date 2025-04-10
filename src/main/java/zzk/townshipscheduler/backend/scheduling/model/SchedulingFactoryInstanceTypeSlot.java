package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
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
@JsonIgnoreType
public class SchedulingFactoryInstanceTypeSlot extends BaseSchedulingFactoryInstance {

    @JsonIgnore
    @InverseRelationShadowVariable(sourceVariableName = SchedulingProducingArrangementFactoryTypeSlot.PLANNING_FACTORY)
    private List<SchedulingProducingArrangementFactoryTypeSlot> producingArrangementFactorySlotList = new ArrayList<>();

    @Override
    public List<ArrangeConsequence> useFilteredArrangeConsequences() {
        return producingArrangementFactorySlotList.stream()
                .map(SchedulingProducingArrangementFactoryTypeSlot::calcConsequence)
                .flatMap(Collection::stream)
                .filter(consequence -> consequence.getResource().getRoot() == this)
                .filter(consequence -> consequence.getResource() instanceof ArrangeConsequence.FactoryProducingLength)
                .sorted()
                .toList();
    }

}
