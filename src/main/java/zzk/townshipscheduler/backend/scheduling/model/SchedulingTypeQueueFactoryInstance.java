package zzk.townshipscheduler.backend.scheduling.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class SchedulingTypeQueueFactoryInstance
        extends BaseSchedulingFactoryInstance
        implements ISchedulingFactoryOrFactoryArrangement {

    public static final String SHADOW_ACTION_CONSEQUENCES = "shadowActionConsequences";

    private SchedulingFactoryQueueProducingArrangement nextQueueProducingArrangement;

    @Override
    public LocalDateTime getCompletedDateTime() {
        var producingArrangement = this.getNextQueueProducingArrangement();
        if (producingArrangement == null) {
            return null;
        }
        while (producingArrangement.getNextQueueProducingArrangement() != null) {
            producingArrangement = producingArrangement.getNextQueueProducingArrangement();
        }
        return producingArrangement.getCompletedDateTime();
    }

    @Override
    public boolean remainProducingLengthHadIllegal() {
        List<ActionConsequence> actionConsequences = new ArrayList<>();
        SchedulingFactoryQueueProducingArrangement queueProducingArrangement = nextQueueProducingArrangement;
        while (queueProducingArrangement != null) {
            actionConsequences.addAll(queueProducingArrangement.calcConsequence());
            queueProducingArrangement = queueProducingArrangement.getNextQueueProducingArrangement();
        }

        var resourceChanges = actionConsequences.stream()
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

    @Override
    public SchedulingFactoryInfo getFactoryInfo() {
        return super.getSchedulingFactoryInfo();
    }

}
