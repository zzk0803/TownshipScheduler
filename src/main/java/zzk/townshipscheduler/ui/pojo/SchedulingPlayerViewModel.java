package zzk.townshipscheduler.ui.pojo;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayer;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * DTO for {@link SchedulingPlayer}
 */
public record SchedulingPlayerViewModel(
        String id,
        LocalTime sleepStart,
        LocalTime sleepEnd
) implements Serializable {

}
