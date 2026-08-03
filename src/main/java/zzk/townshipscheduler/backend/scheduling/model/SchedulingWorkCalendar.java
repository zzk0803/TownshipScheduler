package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SchedulingWorkCalendar
        implements Serializable {

    @Serial
    private static final long serialVersionUID = -6404198941779623689L;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime startDateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDateTime;

    public SchedulingWorkCalendar(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public static SchedulingWorkCalendar with(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return new SchedulingWorkCalendar(startDateTime, endDateTime);
    }

}
