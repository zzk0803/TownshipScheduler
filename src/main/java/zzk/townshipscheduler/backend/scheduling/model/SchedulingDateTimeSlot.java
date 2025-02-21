package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class SchedulingDateTimeSlot implements Comparable<SchedulingDateTimeSlot> {

    private static ThreadLocal<Set<SchedulingDateTimeSlot>> cached = new ThreadLocal<>();

    private Integer id;

    private LocalDateTime start;

    private LocalDateTime end;

    private int durationInMinute;

//    public static CountableValueRange<LocalDateTime> toValueRange(
//            final LocalDateTime startInclusive,
//            final LocalDateTime endExclusive,
//            final int durationInMinute
//    ) {
//        return ValueRangeFactory.createLocalDateTimeValueRange(
//                startInclusive,
//                endExclusive,
//                durationInMinute,
//                ChronoUnit.MINUTES
//        );
//    }

    public static Optional<SchedulingDateTimeSlot> of(
            final LocalDateTime dateTime
    ) {
        Set<SchedulingDateTimeSlot> dateTimeSlots = cached.get();
        for (SchedulingDateTimeSlot currentSlot : dateTimeSlots) {
            LocalDateTime currentSlotStart = currentSlot.getStart();
            LocalDateTime currentSlotEnd = currentSlot.getEnd();
            if (isDateTimeBetween(dateTime, currentSlotStart, currentSlotEnd)) {
                return Optional.of(currentSlot);
            }
        }
        return Optional.empty();
    }

    private static boolean isDateTimeBetween(
            LocalDateTime dateTime,
            LocalDateTime formerDateTime,
            LocalDateTime latterDateTime
    ) {
        return (formerDateTime.isEqual(dateTime) || formerDateTime.isBefore(dateTime))
               && latterDateTime.isAfter(dateTime);
    }

    public static Set<SchedulingDateTimeSlot> toValueRange(
            final LocalDateTime startInclusive,
            final LocalDateTime endExclusive,
            final int durationInMinute
    ) {
        Set<SchedulingDateTimeSlot> result = new LinkedHashSet<>();
        long minutesNumber = startInclusive.until(endExclusive, ChronoUnit.MINUTES);
        long slot = minutesNumber / durationInMinute;
        slot = slot + (minutesNumber % durationInMinute > 0 ? 1 : 0);
        LocalDateTime slotStart = startInclusive;
        LocalDateTime slotEnd = startInclusive.plusMinutes(durationInMinute);
        AtomicInteger idRoller = new AtomicInteger(1);
        for (long i = 0; i < slot; i++) {
            SchedulingDateTimeSlot schedulingDateTimeSlot = new SchedulingDateTimeSlot();
            schedulingDateTimeSlot.setId(idRoller.getAndIncrement());
            schedulingDateTimeSlot.setStart(slotStart);
            schedulingDateTimeSlot.setEnd(slotEnd);
            schedulingDateTimeSlot.setDurationInMinute(durationInMinute);
            result.add(schedulingDateTimeSlot);
            slotStart = slotStart.plusMinutes(durationInMinute);
            slotEnd = slotEnd.plusMinutes(durationInMinute);
        }
        cached.set(result);
        return result;
    }

    @Override
    public String toString() {
        return start.toString() + "~" + end.toString();
    }

    @Override
    public int compareTo(SchedulingDateTimeSlot that) {
        return Comparator.comparing(SchedulingDateTimeSlot::getStart)
//                .thenComparing(SchedulingDateTimeSlot::getEnd)
//                .thenComparingInt(SchedulingDateTimeSlot::getId)
                .compare(this, that);
    }

}
