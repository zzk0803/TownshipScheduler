package zzk.townshipscheduler.backend.scheduling.model;

import java.time.LocalDateTime;

public record FactoryComputedDateTimeTuple(
        LocalDateTime producingDateTime,
        LocalDateTime completedDateTime
) {

}
