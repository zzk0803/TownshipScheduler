package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FactoryComputedDateTimePair {

    private LocalDateTime producingDateTime;

    private LocalDateTime completedDateTime;

    public FactoryComputedDateTimePair() {
    }

    public FactoryComputedDateTimePair(LocalDateTime producingDateTime, LocalDateTime completedDateTime) {
        this.producingDateTime = producingDateTime;
        this.completedDateTime = completedDateTime;
    }


}
