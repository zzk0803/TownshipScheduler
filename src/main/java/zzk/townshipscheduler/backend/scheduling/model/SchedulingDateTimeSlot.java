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
public class SchedulingDateTimeSlot implements IActionSensitive {

    public static final Comparator<SchedulingDateTimeSlot> DATE_TIME_SLOT_COMPARATOR
            = Comparator.comparing(SchedulingDateTimeSlot::getStart);

    private static ThreadLocal<List<SchedulingDateTimeSlot>> cached = new ThreadLocal<>();

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

    public static Optional<SchedulingDateTimeSlot> of(
            final LocalDateTime dateTime
    ) {
        List<SchedulingDateTimeSlot> dateTimeSlots = cached.get();
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
        for (long i = 0; i < slot; i++) {
            SchedulingDateTimeSlot schedulingDateTimeSlot = new SchedulingDateTimeSlot();
            schedulingDateTimeSlot.setId(idRoller.incrementAndGet());
            schedulingDateTimeSlot.setStart(slotStart);
            schedulingDateTimeSlot.setEnd(slotEnd);
            schedulingDateTimeSlot.setDurationInMinute(durationInMinute);
            result.add(schedulingDateTimeSlot);

            slotStart = slotStart.plus(durationInMinute, ChronoUnit.MINUTES);
            slotEnd = slotEnd.plus(durationInMinute, ChronoUnit.MINUTES);
        }
        cached.set(result);
        return result;
    }

    @Override
    public String toString() {
        return start.toString() + "~" + end.toString();
    }

    public Comparator<SchedulingDateTimeSlot> getDateTimeSlotComparator() {
        return DATE_TIME_SLOT_COMPARATOR;
    }

}
