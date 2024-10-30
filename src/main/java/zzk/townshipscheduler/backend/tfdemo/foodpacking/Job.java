package zzk.townshipscheduler.backend.tfdemo.foodpacking;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Duration;
import java.time.LocalDateTime;

@PlanningEntity
public class Job {

    @PlanningId
    private String id;

    private String name;

    private Product product;

    private Duration duration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime minStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime idealEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime maxEndTime;

    /**
     * Higher priority is a higher number.
     */
    private int priority;

    @PlanningPin
    private boolean pinned;

    @InverseRelationShadowVariable(sourceVariableName = "jobs")
    private Line line;

    @JsonIgnore
    @PreviousElementShadowVariable(sourceVariableName = "jobs")
    private Job previousJob;

    @JsonIgnore
    @NextElementShadowVariable(sourceVariableName = "jobs")
    private Job nextJob;

    /**
     * Start is after cleanup.
     */
    @CascadingUpdateShadowVariable(targetMethodName = "updateStartCleaningDateTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startCleaningDateTime;

    @CascadingUpdateShadowVariable(targetMethodName = "updateStartCleaningDateTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startProductionDateTime;

    @CascadingUpdateShadowVariable(targetMethodName = "updateStartCleaningDateTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDateTime;

    // No-arg constructor required for Timefold
    public Job() {
    }

    public Job(
            String id,
            String name,
            Product product,
            Duration duration,
            LocalDateTime minStartTime,
            LocalDateTime idealEndTime,
            LocalDateTime maxEndTime,
            int priority,
            boolean pinned
    ) {
        this(id, name, product, duration, minStartTime, idealEndTime, maxEndTime, priority, pinned, null, null);
    }

    public Job(
            String id,
            String name,
            Product product,
            Duration duration,
            LocalDateTime minStartTime,
            LocalDateTime idealEndTime,
            LocalDateTime maxEndTime,
            int priority,
            boolean pinned,
            LocalDateTime startCleaningDateTime,
            LocalDateTime startProductionDateTime
    ) {
        this.id = id;
        this.name = name;
        this.product = product;
        this.duration = duration;
        this.minStartTime = minStartTime;
        this.idealEndTime = idealEndTime;
        this.maxEndTime = maxEndTime;
        this.priority = priority;
        this.startCleaningDateTime = startCleaningDateTime;
        this.startProductionDateTime = startProductionDateTime;
        this.endDateTime = startProductionDateTime == null ? null : startProductionDateTime.plus(duration);
        this.pinned = pinned;
    }

    @Override
    public String toString() {
        return id + "(" + product.getName() + ")";
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public LocalDateTime getMinStartTime() {
        return minStartTime;
    }

    public LocalDateTime getIdealEndTime() {
        return idealEndTime;
    }

    public LocalDateTime getMaxEndTime() {
        return maxEndTime;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isPinned() {
        return pinned;
    }

    public Job getNextJob() {
        return nextJob;
    }

    public void setNextJob(Job nextJob) {
        this.nextJob = nextJob;
    }

    public LocalDateTime getStartProductionDateTime() {
        return startProductionDateTime;
    }

    @SuppressWarnings("unused")
    private void updateStartCleaningDateTime() {
        if (getLine() == null) {
            if (getStartCleaningDateTime() != null) {
                setStartCleaningDateTime(null);
                setStartProductionDateTime(null);
                setEndDateTime(null);
            }
            return;
        }

        Job previous = getPreviousJob();
        LocalDateTime startCleaning;
        LocalDateTime startProduction;
        if (previous == null) {
            startCleaning = line.getStartDateTime();
            startProduction = line.getStartDateTime();
        } else {
            startCleaning = previous.getEndDateTime();
            startProduction = startCleaning == null
                    ? null
                    : startCleaning.plus(getProduct().getCleanupDuration(previous.getProduct()));
        }
        setStartCleaningDateTime(startCleaning);
        setStartProductionDateTime(startProduction);
        var endTime = startProduction == null ? null : startProduction.plus(getDuration());
        setEndDateTime(endTime);
    }

    public void setStartProductionDateTime(LocalDateTime startProductionDateTime) {
        this.startProductionDateTime = startProductionDateTime;
    }

    public Line getLine() {
        return line;
    }

    public void setLine(Line line) {
        this.line = line;
    }

    public LocalDateTime getStartCleaningDateTime() {
        return startCleaningDateTime;
    }

    public void setStartCleaningDateTime(LocalDateTime startCleaningDateTime) {
        this.startCleaningDateTime = startCleaningDateTime;
    }

    public Job getPreviousJob() {
        return previousJob;
    }

    public void setPreviousJob(Job previousJob) {
        this.previousJob = previousJob;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public Product getProduct() {
        return product;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    public Duration getDuration() {
        return duration;
    }

}
