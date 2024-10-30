package zzk.townshipscheduler.backend.tfdemo.projectscheduling;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public class PredecessorsDoneDateUpdatingVariableListener
        implements VariableListener<ProjectJobSchedule, Allocation> {

    @Override
    public void beforeEntityAdded(ScoreDirector<ProjectJobSchedule> scoreDirector, Allocation allocation) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<ProjectJobSchedule> scoreDirector, Allocation allocation) {
        updateAllocation(scoreDirector, allocation);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<ProjectJobSchedule> scoreDirector, Allocation allocation) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<ProjectJobSchedule> scoreDirector, Allocation allocation) {
        // Do nothing
    }

    protected void updateAllocation(ScoreDirector<ProjectJobSchedule> scoreDirector, Allocation originalAllocation) {
        // Reset computed variables when a planning variable changes to prevent score corruption
        originalAllocation.invalidateComputedVariables();
        Queue<Allocation> uncheckedSuccessorQueue = new ArrayDeque<>();
        uncheckedSuccessorQueue.addAll(originalAllocation.getSuccessorAllocations());
        while (!uncheckedSuccessorQueue.isEmpty()) {
            Allocation allocation = uncheckedSuccessorQueue.remove();
            boolean updated = updatePredecessorsDoneDate(scoreDirector, allocation);
            if (updated) {
                uncheckedSuccessorQueue.addAll(allocation.getSuccessorAllocations());
            }
        }
    }

    /**
     * @param scoreDirector never null
     * @param allocation    never null
     * @return true if the startDate changed
     */
    protected boolean updatePredecessorsDoneDate(
            ScoreDirector<ProjectJobSchedule> scoreDirector,
            Allocation allocation
    ) {
        // For the source the doneDate must be 0.
        Integer doneDate = 0;
        for (Allocation predecessorAllocation : allocation.getPredecessorAllocations()) {
            int endDate = predecessorAllocation.getEndDate();
            doneDate = Math.max(doneDate, endDate);
        }
        if (Objects.equals(doneDate, allocation.getPredecessorsDoneDate())) {
            return false;
        }
        scoreDirector.beforeVariableChanged(allocation, "predecessorsDoneDate");
        allocation.setPredecessorsDoneDate(doneDate);
        scoreDirector.afterVariableChanged(allocation, "predecessorsDoneDate");
        return true;
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<ProjectJobSchedule> scoreDirector, Allocation allocation) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<ProjectJobSchedule> scoreDirector, Allocation allocation) {
        updateAllocation(scoreDirector, allocation);
    }

}
