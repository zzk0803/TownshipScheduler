package zzk.townshipscheduler.backend.tfdemo.projectscheduling;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingDouble;

public class ExecutionModeStrengthWeightFactory
        implements SelectionSorterWeightFactory<ProjectJobSchedule, ExecutionMode> {

    @Override
    public ExecutionModeStrengthWeight createSorterWeight(
            ProjectJobSchedule projectJobSchedule,
            ExecutionMode executionMode
    ) {
        Map<Resource, Integer> requirementTotalMap = new HashMap<>(
                executionMode.getResourceRequirements().size()
        );
        for (ResourceRequirement resourceRequirement : executionMode.getResourceRequirements()) {
            requirementTotalMap.put(resourceRequirement.getResource(), 0);
        }
        for (ResourceRequirement resourceRequirement : projectJobSchedule.getResourceRequirements()) {
            Resource resource = resourceRequirement.getResource();
            Integer total = requirementTotalMap.get(resource);
            if (total != null) {
                total += resourceRequirement.getRequirement();
                requirementTotalMap.put(resource, total);
            }
        }
        double requirementDesirability = 0.0;
        for (ResourceRequirement resourceRequirement : executionMode.getResourceRequirements()) {
            Resource resource = resourceRequirement.getResource();
            int total = requirementTotalMap.get(resource);
            if (total > resource.getCapacity()) {
                requirementDesirability += (total - resource.getCapacity())
                                           * (double) resourceRequirement.getRequirement()
                                           * (resource.isRenewable() ? 1.0 : 100.0);
            }
        }
        return new ExecutionModeStrengthWeight(executionMode, requirementDesirability);
    }

    public static class ExecutionModeStrengthWeight
            implements Comparable<ExecutionModeStrengthWeight> {

        private static final Comparator<ExecutionModeStrengthWeight> COMPARATOR = comparingDouble(
                (ExecutionModeStrengthWeight weight) -> weight.requirementDesirability)
                .thenComparing(weight -> weight.executionMode, comparing(ExecutionMode::getId));

        private final ExecutionMode executionMode;

        private final double requirementDesirability;

        public ExecutionModeStrengthWeight(ExecutionMode executionMode, double requirementDesirability) {
            this.executionMode = executionMode;
            this.requirementDesirability = requirementDesirability;
        }

        @Override
        public int compareTo(ExecutionModeStrengthWeight other) {
            return COMPARATOR.compare(this, other);
        }

    }

}
