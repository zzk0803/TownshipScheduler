package zzk.townshipscheduler.backend.scheduling.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SchedulingWorkCalendar {

    private final LocalDateTime startDateTime;

    private final LocalDateTime endDateTime;

}
