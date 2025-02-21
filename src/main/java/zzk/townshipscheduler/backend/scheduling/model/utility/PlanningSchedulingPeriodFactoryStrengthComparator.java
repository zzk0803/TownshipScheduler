//package zzk.townshipscheduler.backend.scheduling.model.utility;
//
//import org.apache.commons.lang3.builder.CompareToBuilder;
//import org.springframework.util.Assert;
//import zzk.townshipscheduler.backend.scheduling.model.ActionConsequence;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryTimeSlotInstance;
//
//import java.util.Comparator;
//
//public class PlanningSchedulingPeriodFactoryStrengthComparator implements Comparator<SchedulingFactoryTimeSlotInstance> {
//
//    @Override
//    public int compare(SchedulingFactoryTimeSlotInstance factory1, SchedulingFactoryTimeSlotInstance factory2) {
//        Assert.isTrue(
//                factory1.getSchedulingFactoryInfo() == factory2.getSchedulingFactoryInfo(),
//                "should be same factory type"
//        );
//
//        Integer factory1QueueRemain = factory1.calcFactoryConsequence().stream()
//                .filter(actionConsequence -> actionConsequence.getResource() instanceof ActionConsequence.FactoryWaitQueue)
//                .map(ActionConsequence::getResourceChange)
//                .reduce(
//                        factory1.getProducingLength(),
//                        (integer, schedulingResourceChange) -> schedulingResourceChange.apply(integer),
//                        Integer::sum
//                );
//        Integer factory2QueueRemain = factory2.calcFactoryConsequence().stream()
//                .filter(actionConsequence -> actionConsequence.getResource() instanceof ActionConsequence.FactoryWaitQueue)
//                .map(ActionConsequence::getResourceChange)
//                .reduce(
//                        factory1.getProducingLength(),
//                        (integer, schedulingResourceChange) -> schedulingResourceChange.apply(integer),
//                        Integer::sum
//                );
//
//        return new CompareToBuilder()
//                .append(
//                        factory1, factory2,
//                        Comparator.comparing(SchedulingFactoryTimeSlotInstance::getDateTimeSlot).reversed()
//                )
//                .append(factory1QueueRemain, factory2QueueRemain)
//                .toComparison();
//    }
//
//}
