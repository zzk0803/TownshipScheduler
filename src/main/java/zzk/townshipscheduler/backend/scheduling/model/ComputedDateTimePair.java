package zzk.townshipscheduler.backend.scheduling.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;

public record ComputedDateTimePair(
        LocalDateTime producingDateTime,
        LocalDateTime completedDateTime
)
        implements Serializable, Comparable<ComputedDateTimePair> {

    @Override
    public int compareTo(ComputedDateTimePair that) {
        return Comparator.comparing(ComputedDateTimePair::producingDateTime)
                .thenComparing(ComputedDateTimePair::completedDateTime)
                .compare(
                        this,
                        that
                )
                ;
    }

}
