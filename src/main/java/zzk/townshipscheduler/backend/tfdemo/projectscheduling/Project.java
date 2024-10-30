package zzk.townshipscheduler.backend.tfdemo.projectscheduling;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.Objects;

@JsonIdentityInfo(scope = Project.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Project {

    private String id;

    private int releaseDate;

    private int criticalPathDuration;

    public Project() {
    }

    public Project(String id, int releaseDate, int criticalPathDuration) {
        this(id);
        this.releaseDate = releaseDate;
        this.criticalPathDuration = criticalPathDuration;
    }

    public Project(String id) {
        this.id = id;
    }

    public int getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(int releaseDate) {
        this.releaseDate = releaseDate;
    }

    public int getCriticalPathDuration() {
        return criticalPathDuration;
    }

    public void setCriticalPathDuration(int criticalPathDuration) {
        this.criticalPathDuration = criticalPathDuration;
    }

    @JsonIgnore
    public int getCriticalPathEndDate() {
        return releaseDate + criticalPathDuration;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Project project))
            return false;
        return Objects.equals(getId(), project.getId());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
