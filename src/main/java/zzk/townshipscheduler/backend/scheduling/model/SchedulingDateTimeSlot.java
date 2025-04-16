package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SchedulingDateTimeSlot implements Comparable<SchedulingDateTimeSlot> {

    public static final Comparator<SchedulingDateTimeSlot> DATE_TIME_SLOT_COMPARATOR
            = Comparator.comparing(SchedulingDateTimeSlot::getStart);

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    private LocalDateTime start;

    private LocalDateTime end;

    private int durationInMinute;

//    @ShadowVariable(
//            sourceVariableName = PLANNING_ACTIONS,
//            variableListenerClass = DataTimeSlotRollingAndPushVariableListener.class
//    )
//    private Long shadowRollingChange = 0L;

    private static boolean isDateTimeBetween(
            LocalDateTime dateTime,
            LocalDateTime formerDateTime,
            LocalDateTime latterDateTime
    ) {
        return (formerDateTime.isEqual(dateTime) || formerDateTime.isBefore(dateTime))
               && latterDateTime.isAfter(dateTime);
    }

    public static Optional<SchedulingDateTimeSlot> fromRangeJumpCeil(
            List<SchedulingDateTimeSlot> range,
            LocalDateTime localDateTime
    ) {
        if (range == null || range.isEmpty()) {
            throw new IllegalArgumentException();
        }

        if (localDateTime.isAfter(range.getLast().getStart())) {
            return Optional.empty();
        }

        return range.stream()
                .filter(iteratingSlot -> iteratingSlot.getStart().isAfter(localDateTime))
                .limit(3)
                .findAny();
    }

    public static List<SchedulingDateTimeSlot> toValueRange(
            final LocalDateTime startInclusive,
            final LocalDateTime endExclusive,
            final int durationInMinute
    ) {
        int minutesNumber = Math.toIntExact(startInclusive.until(endExclusive, ChronoUnit.MINUTES));
        int slot = minutesNumber / durationInMinute;
        slot = slot + (minutesNumber % durationInMinute > 0 ? 1 : 0);
        List<SchedulingDateTimeSlot> result = new ArrayList<>(slot);
        LocalDateTime slotStart = startInclusive;
        LocalDateTime slotEnd = startInclusive.plusMinutes(durationInMinute);
        AtomicInteger idRoller = new AtomicInteger(0);
        SchedulingDateTimeSlot schedulingDateTimeSlot;
        for (long i = 0; i < slot; i++) {
            schedulingDateTimeSlot = new SchedulingDateTimeSlot();
            schedulingDateTimeSlot.setId(idRoller.incrementAndGet());
            schedulingDateTimeSlot.setStart(slotStart);
            schedulingDateTimeSlot.setEnd(slotEnd);
            schedulingDateTimeSlot.setDurationInMinute(durationInMinute);
            result.add(schedulingDateTimeSlot);

            slotStart = slotStart.plusMinutes(durationInMinute);
            slotEnd = slotEnd.plusMinutes(durationInMinute);
        }
        return result;
    }

    @Override
    public String toString() {
        return start.toString() + "~" + end.toString();
    }

    @Override
    public int compareTo(SchedulingDateTimeSlot that) {
        return getDateTimeSlotComparator().compare(this, that);
    }

    public Comparator<SchedulingDateTimeSlot> getDateTimeSlotComparator() {
        return DATE_TIME_SLOT_COMPARATOR;
    }

}
