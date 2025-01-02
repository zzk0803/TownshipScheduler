package zzk.townshipscheduler.backend.scheduling.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SchedulingWorkTimeLimit {

    private final LocalDateTime startDateTime;

    private final LocalDateTime endDateTime;

}
