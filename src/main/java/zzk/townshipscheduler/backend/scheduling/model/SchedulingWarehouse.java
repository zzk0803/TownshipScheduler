package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SchedulingWarehouse {

    @PlanningId
    @EqualsAndHashCode.Include
    private String id = "warehouse";

    private Map<SchedulingProduct, Integer> productAmountMap;

    public Stream<ActionConsequence> streamProductStockConsequence(List<SchedulingPlayerFactoryAction> actions) {
        return actions.stream()
                .map(SchedulingPlayerFactoryAction::calcActionConsequence)
                .flatMap(Collection::stream)
                .sorted()
                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.ProductStock);
    }

}
