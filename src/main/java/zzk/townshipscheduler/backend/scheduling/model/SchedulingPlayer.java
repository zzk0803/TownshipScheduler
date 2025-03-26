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

    private LocalTime sleepStart = LocalTime.MIDNIGHT.minusHours(2);

    private LocalTime sleepEnd = LocalTime.MIDNIGHT.plusHours(7);

    private Map<SchedulingProduct, Integer> productAmountMap;

    public Map<SchedulingProduct, Integer> mergeToProductAmountMap(List<BaseProducingArrangement> producingArrangements) {
        List<ActionConsequence> actionConsequenceList = mapToActionProductStockConsequences(producingArrangements);
        Map<SchedulingProduct, Integer> productStockMap = new LinkedHashMap<>(productAmountMap);
        for (ActionConsequence consequence : actionConsequenceList) {
            ActionConsequence.ProductStock productStock = (ActionConsequence.ProductStock) consequence.getResource();
            ActionConsequence.SchedulingResourceChange resourceChange = consequence.getResourceChange();
            SchedulingProduct schedulingProduct = productStock.getSchedulingProduct();
            Integer stock = productStockMap.getOrDefault(schedulingProduct, 0);
            stock = resourceChange.apply(stock);
            productStockMap.put(schedulingProduct, stock);
        }
        return productStockMap;
    }

    public List<ActionConsequence> mapToActionProductStockConsequences(List<BaseProducingArrangement> producingArrangements) {
        return producingArrangements.stream()
                .map(BaseProducingArrangement::calcConsequence)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(ActionConsequence::getLocalDateTime))
                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.ProductStock)
                .toList();
    }

    public boolean remainStockHadIllegal(List<BaseProducingArrangement> producingArrangements) {
        List<ActionConsequence> actionConsequences = mapToActionProductStockConsequences(producingArrangements);
        Map<SchedulingProduct, Integer> productStockMap = new LinkedHashMap<>(productAmountMap);
        for (ActionConsequence consequence : actionConsequences) {
            ActionConsequence.ProductStock productStock = (ActionConsequence.ProductStock) consequence.getResource();
            ActionConsequence.SchedulingResourceChange resourceChange = consequence.getResourceChange();
            SchedulingProduct schedulingProduct = productStock.getSchedulingProduct();
            Integer stock = productStockMap.getOrDefault(schedulingProduct, 0);
            stock = resourceChange.apply(stock);
            if (stock < 0) {
                return true;
            }
            productStockMap.put(schedulingProduct, stock);
        }
        return false;
    }

}
