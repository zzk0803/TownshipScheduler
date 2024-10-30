package zzk.townshipscheduler.backend.tfdemo.projectscheduling;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

public class NotSourceOrSinkAllocationFilter
        implements PinningFilter<ProjectJobSchedule, Allocation> {

    @Override
    public boolean accept(ProjectJobSchedule projectJobSchedule, Allocation allocation) {
        JobType jobType = allocation.getJob().getJobType();
        return jobType == JobType.SOURCE || jobType == JobType.SINK;
    }

}
