package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SchedulingPlayer {

    @PlanningId
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id = "warehouse";

    private Map<SchedulingProduct, Integer> productAmountMap;

    private Map<BaseProducingArrangement, List<ActionConsequence>> acceptedActionConsequencesMap
            = new LinkedHashMap<>();

    public void acceptActionConsequence(
            BaseProducingArrangement producingArrangement, List<ActionConsequence> actionConsequences
    ) {
        acceptedActionConsequencesMap.put(producingArrangement, actionConsequences);
    }

    public Map<SchedulingProduct, Integer> toProductAmountMap(BaseProducingArrangement beforeThisArrangement) {
        List<ActionConsequence> sortedMappedConsequences
                = acceptedActionConsequencesMap.values().stream()
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(ActionConsequence::getLocalDateTime))
                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.ProductStock)
                .takeWhile(consequence -> consequence.getLocalDateTime()
                        .isBefore(beforeThisArrangement.getPlanningDateTimeSlotStartAsLocalDateTime())
                )
                .toList();
        Map<SchedulingProduct, Integer> productStockMap = new LinkedHashMap<>();
        for (ActionConsequence consequence : sortedMappedConsequences) {
            ActionConsequence.ProductStock productStock = (ActionConsequence.ProductStock) consequence.getResource();
            ActionConsequence.SchedulingResourceChange resourceChange = consequence.getResourceChange();
            SchedulingProduct schedulingProduct = productStock.getSchedulingProduct();
            Integer stock = productStockMap.getOrDefault(schedulingProduct, 0);
            stock = resourceChange.apply(stock);
            productStockMap.put(schedulingProduct, stock);
        }
        return productStockMap;
    }

    public Map<SchedulingProduct, Integer> toProductAmountMap() {
        List<ActionConsequence> sortedMappedConsequences = acceptedActionConsequencesMap.values().stream()
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(ActionConsequence::getLocalDateTime))
                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.ProductStock)
                .toList();
        Map<SchedulingProduct, Integer> productStockMap = new LinkedHashMap<>();
        for (ActionConsequence consequence : sortedMappedConsequences) {
            ActionConsequence.ProductStock productStock = (ActionConsequence.ProductStock) consequence.getResource();
            ActionConsequence.SchedulingResourceChange resourceChange = consequence.getResourceChange();
            SchedulingProduct schedulingProduct = productStock.getSchedulingProduct();
            Integer stock = productStockMap.getOrDefault(schedulingProduct, 0);
            stock = resourceChange.apply(stock);
            productStockMap.put(schedulingProduct, stock);
        }
        return productStockMap;
    }

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

}
