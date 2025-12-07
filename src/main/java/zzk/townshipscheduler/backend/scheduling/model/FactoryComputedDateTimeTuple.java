package zzk.townshipscheduler.backend.scheduling.model;

import java.time.LocalDateTime;

public record FactoryComputedDateTimeTuple(
        FactoryProcessSequence factoryProcessSequence,
        LocalDateTime producingDateTime,
        LocalDateTime completedDateTime
) {

}
