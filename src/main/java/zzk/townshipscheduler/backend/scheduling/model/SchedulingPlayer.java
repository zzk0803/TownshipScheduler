package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalTime;
import java.util.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SchedulingPlayer {

    @PlanningId
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id = "warehouse";

    private LocalTime sleepStart = LocalTime.of(22, 30);

    private LocalTime sleepEnd = LocalTime.of(7, 30);

    private Map<SchedulingProduct, Integer> productAmountMap;


    public Map<SchedulingProduct, Integer> toProductAmountMap(List<ArrangeConsequence> arrangeConsequenceList) {
        Map<SchedulingProduct, Integer> productStockMap = new LinkedHashMap<>(productAmountMap);
        for (ArrangeConsequence consequence : arrangeConsequenceList) {
            ArrangeConsequence.ProductStock productStock = (ArrangeConsequence.ProductStock) consequence.getResource();
            ArrangeConsequence.SchedulingResourceChange resourceChange = consequence.getResourceChange();
            SchedulingProduct schedulingProduct = productStock.getSchedulingProduct();
            Integer stock = productStockMap.getOrDefault(schedulingProduct, 0);
            stock = resourceChange.apply(stock);
            productStockMap.put(schedulingProduct, stock);
        }
        return productStockMap;
    }

    public List<ArrangeConsequence> toActionProductStockConsequences(List<BaseSchedulingProducingArrangement> producingArrangements) {
        return producingArrangements.stream()
                .map(BaseSchedulingProducingArrangement::calcConsequence)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(ArrangeConsequence::getLocalDateTime))
                .filter(consequence -> consequence.getResource() instanceof ArrangeConsequence.ProductStock)
                .toList();
    }

}
