package zzk.townshipscheduler.ui.pojo.scheduling;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link SchedulingDateTimeSlot}
 */
public record SchedulingDateTimeSlotViewModel(
        int id,
        LocalDateTime start,
        LocalDateTime end
) implements Serializable {

}
