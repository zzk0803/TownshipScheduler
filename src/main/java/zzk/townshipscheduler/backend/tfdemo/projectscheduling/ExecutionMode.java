package zzk.townshipscheduler.backend.tfdemo.projectscheduling;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;
import java.util.Objects;

@JsonIdentityInfo(scope = ExecutionMode.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class ExecutionMode {

    @PlanningId
    private String id;

    private Job job;

    private int duration; // In days

    private List<ResourceRequirement> resourceRequirements;

    public ExecutionMode() {
    }

    public ExecutionMode(String id, Job job, int duration, List<ResourceRequirement> resourceRequirements) {
        this(id, job, duration);
        this.resourceRequirements = resourceRequirements;
    }

    public ExecutionMode(String id, Job job, int duration) {
        this(id);
        this.job = job;
        this.duration = duration;
    }

    public ExecutionMode(String id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public List<ResourceRequirement> getResourceRequirements() {
        return resourceRequirements;
    }

    public void setResourceRequirements(List<ResourceRequirement> resourceRequirements) {
        this.resourceRequirements = resourceRequirements;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ExecutionMode that))
            return false;
        return Objects.equals(getId(), that.getId());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
