package zzk.townshipscheduler.backend.scheduling.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;

public record FactoryComputedDateTimePair(
        LocalDateTime producingDateTime,
        LocalDateTime completedDateTime
)
        implements Serializable, Comparable<FactoryComputedDateTimePair> {

    @Override
    public int compareTo(FactoryComputedDateTimePair that) {
        return Comparator.comparing(FactoryComputedDateTimePair::producingDateTime)
                .thenComparing(FactoryComputedDateTimePair::completedDateTime)
                .compare(
                        this,
                        that
                )
                ;
    }

}
