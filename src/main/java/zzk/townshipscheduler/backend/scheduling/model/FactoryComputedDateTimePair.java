package zzk.townshipscheduler.backend.scheduling.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record FactoryComputedDateTimePair(
        LocalDateTime producingDateTime,
        LocalDateTime completedDateTime
) implements Serializable {

}
