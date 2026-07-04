package zzk.townshipscheduler.ui.pojo;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link SchedulingDateTimeSlot}
 */
public record SchedulingDateTimeSlotViewModel(
        Integer id,
        LocalDateTime start,
        LocalDateTime end
) implements Serializable {

}
