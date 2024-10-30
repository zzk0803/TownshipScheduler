package zzk.townshipscheduler.backend.tfdemo.projectscheduling;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.*;

public class ProjectJobSchedulingConstraintProvider
        implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                nonRenewableResourceCapacity(constraintFactory),
                renewableResourceCapacity(constraintFactory),
                totalProjectDelay(constraintFactory),
                totalMakespan(constraintFactory)
        };
    }

    protected Constraint nonRenewableResourceCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ResourceRequirement.class)
                .filter(resource -> !resource.isResourceRenewable())
                .join(
                        Allocation.class,
                        Joiners.equal(ResourceRequirement::getExecutionMode, Allocation::getExecutionMode)
                )
                .groupBy(
                        (requirement, allocation) -> requirement.getResource(),
                        ConstraintCollectors.sum((requirement, allocation) -> requirement.getRequirement())
                )
                .filter((resource, requirements) -> requirements > resource.getCapacity())
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        (resource, requirements) -> requirements - resource.getCapacity()
                )
                .asConstraint("Non-renewable resource capacity");
    }

    protected Constraint renewableResourceCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ResourceRequirement.class)
                .filter(ResourceRequirement::isResourceRenewable)
                .join(
                        Allocation.class,
                        Joiners.equal(ResourceRequirement::getExecutionMode, Allocation::getExecutionMode)
                )
                .flattenLast(Allocation::getBusyDates)
                .groupBy(
                        (resourceReq, date) -> resourceReq.getResource(),
                        (resourceReq, date) -> date,
                        ConstraintCollectors.sum((resourceReq, date) -> resourceReq.getRequirement())
                )
                .filter((resourceReq, date, totalRequirement) -> totalRequirement > resourceReq.getCapacity())
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        (resourceReq, date, totalRequirement) -> totalRequirement - resourceReq.getCapacity()
                )
                .asConstraint("Renewable resource capacity");
    }

    protected Constraint totalProjectDelay(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Allocation.class)
                .filter(allocation -> allocation.getJobType() == JobType.SINK)
                .filter(allocation -> allocation.getEndDate() != null)
                .filter(allocation -> allocation.getProjectDelay() > 0)
                .penalize(HardMediumSoftScore.ONE_MEDIUM, Allocation::getProjectDelay)
                .asConstraint("Total project delay");
    }

    protected Constraint totalMakespan(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Allocation.class)
                .filter(allocation -> allocation.getJobType() == JobType.SINK)
                .filter(allocation -> allocation.getEndDate() != null)
                .groupBy(ConstraintCollectors.max(Allocation::getEndDate))
                .penalize(HardMediumSoftScore.ONE_SOFT, maxEndDate -> maxEndDate)
                .asConstraint("Total makespan");
    }

}
