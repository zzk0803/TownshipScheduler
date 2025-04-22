package zzk.townshipscheduler.backend.scheduling.model;

import java.time.LocalDateTime;

public record FactoryComputedDataTimePair(LocalDateTime producingDateTime, LocalDateTime completedDateTime) {

}
