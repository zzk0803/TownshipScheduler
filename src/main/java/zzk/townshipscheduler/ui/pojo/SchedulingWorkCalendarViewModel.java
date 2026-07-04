package zzk.townshipscheduler.ui.pojo;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingWorkCalendar;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link SchedulingWorkCalendar}
 */
public record SchedulingWorkCalendarViewModel(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) implements Serializable {

}
