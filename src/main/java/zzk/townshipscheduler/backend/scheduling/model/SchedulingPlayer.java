package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalTime;
import java.util.Map;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SchedulingPlayer implements Serializable {

    public static final LocalTime DEFAULT_SLEEP_START = LocalTime.MIDNIGHT.minusHours(2);

    public static final LocalTime DEFAULT_SLEEP_END = LocalTime.MIDNIGHT.plusHours(8);

    @Serial
    private static final long serialVersionUID = -2467531974779697853L;

    @EqualsAndHashCode.Include
    @ToString.Include
    private String id = "test";

    private Map<SchedulingProduct, Integer> productAmountMap;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime sleepStart = DEFAULT_SLEEP_START;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime sleepEnd = DEFAULT_SLEEP_END;

}
