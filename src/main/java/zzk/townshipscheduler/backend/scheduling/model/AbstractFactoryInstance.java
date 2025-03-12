package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.LocalDateTime;
import java.util.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractFactoryInstance implements IActionSensitive {

    @PlanningId
    @EqualsAndHashCode.Include
    protected Integer id;

    protected SchedulingFactoryInfo schedulingFactoryInfo;

    protected int seqNum;

    protected int producingLength;

    protected int reapWindowSize;

    @Override
    public String toString() {
        return this.schedulingFactoryInfo.getCategoryName() + "#" + this.getSeqNum() + ",size=" + this.getProducingLength();
    }

    public Map<Integer, Integer> calcRemainProducingQueueSize(List<AbstractPlayerProducingArrangement> factoryActions) {
        Map<Integer, Integer> actionIdProducingRemainMap = new LinkedHashMap<>();
        int remain = getProducingLength();
        List<ActionConsequence> factoryConsequence = calcFactoryConsequence(factoryActions);
        List<ActionConsequence> filteredConsequences = factoryConsequence.stream()
                .sorted()
                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryProducingQueue)
                .filter(consequence -> consequence.getResource().getRoot() == this)
                .toList();
        for (ActionConsequence consequence : filteredConsequences) {
            Integer actionId = consequence.getActionId();
            ActionConsequence.SchedulingResourceChange resourceChange = consequence.getResourceChange();
            remain = resourceChange.apply(remain);
            actionIdProducingRemainMap.put(actionId, remain);
        }
        return actionIdProducingRemainMap;
    }

    public List<ActionConsequence> calcFactoryConsequence(
            List<AbstractPlayerProducingArrangement> factoryActions
    ) {
        return Collections.checkedList(factoryActions, AbstractPlayerProducingArrangement.class)
                .stream()
                .map(AbstractPlayerProducingArrangement::calcConsequence)
                .flatMap(Collection::stream)
                .toList();
    }

    public int calcRemainProducingQueueSize(
            List<AbstractPlayerProducingArrangement> factoryActions,
            LocalDateTime localDateTime
    ) {
        int producingLength = getProducingLength();
        List<ActionConsequence> factoryConsequence = calcFactoryConsequence(factoryActions);
        return factoryConsequence.stream()
                .sorted()
                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryProducingQueue)
                .filter(consequence -> consequence.getResource().getRoot() == this)
                .takeWhile(consequence -> {
                    boolean before = consequence.getLocalDateTime().isBefore(localDateTime);
                    boolean equal = consequence.getLocalDateTime().isEqual(localDateTime);
                    return before || equal;
                })
                .map(ActionConsequence::getResourceChange)
                .reduce(
                        producingLength,
                        (integer, resourceChange) -> resourceChange.apply(integer),
                        Integer::sum
                );
    }

    public ProducingStructureType getProducingStructureType() {
        return schedulingFactoryInfo.getProducingStructureType();
    }

}
