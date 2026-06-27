package zzk.townshipscheduler.backend.scheduling.model;


import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingDateTimeSlot implements Comparable<SchedulingDateTimeSlot>, Serializable {

    public static final Comparator<SchedulingDateTimeSlot> DATE_TIME_SLOT_COMPARATOR = Comparator.comparing(SchedulingDateTimeSlot::getStart);

    @Serial
    private static final long serialVersionUID = -36055068413393349L;

    @EqualsAndHashCode.Include
    private Integer id;

    private LocalDateTime start;

    private LocalDateTime end;

    private int durationInMinute;

    @InverseRelationShadowVariable(sourceVariableName = SchedulingProducingArrangement.PLANNING_DATE_TIME_SLOT)
    private List<SchedulingProducingArrangement> planningArrangementsSequence = new ArrayList<>();

    public SchedulingDateTimeSlot(
            LocalDateTime start,
            LocalDateTime end
    ) {
        this.start = start;
        this.end = end;
    }

    private static boolean isDateTimeBetween(
            LocalDateTime dateTime,
            LocalDateTime formerDateTime,
            LocalDateTime latterDateTime
    ) {
        return (formerDateTime.isEqual(dateTime) || formerDateTime.isBefore(dateTime)) && latterDateTime.isAfter(dateTime);
    }

    public static Optional<SchedulingDateTimeSlot> fromRangeJumpCeil(
            List<SchedulingDateTimeSlot> range,
            LocalDateTime localDateTime
    ) {
        if (range == null || range.isEmpty()) {
            throw new IllegalArgumentException();
        }

        if (localDateTime.isAfter(range.getLast()
                                          .getStart())) {
            return Optional.empty();
        }

        return range.stream()
                .filter(iteratingSlot -> iteratingSlot.getStart()
                        .isAfter(localDateTime))
                .limit(1)
                .findFirst();
    }

    public static SchedulingDateTimeSlot ceilingDateTimeFromValueRange(
            NavigableSet<SchedulingDateTimeSlot> valueRange,
            LocalDateTime localDateTime
    ) {
        Objects.requireNonNull(localDateTime);
        Objects.requireNonNull(valueRange);
        return valueRange.ceiling(new SchedulingDateTimeSlot(localDateTime, localDateTime));
    }

    public static TreeSet<SchedulingDateTimeSlot> toValueRange(
            final LocalDateTime startInclusive,
            final LocalDateTime endExclusive,
            final int durationInMinute
    ) {
        int slot = calcLocalDateTimePairSlotCount(startInclusive, endExclusive, durationInMinute);
        TreeSet<SchedulingDateTimeSlot> dateTimeSlotTreeSet = new TreeSet<>();
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

            dateTimeSlotTreeSet.add(schedulingDateTimeSlot);

            slotStart = slotStart.plusMinutes(durationInMinute);
            slotEnd = slotEnd.plusMinutes(durationInMinute);
        }
        return dateTimeSlotTreeSet;
    }

    public static int calcLocalDateTimePairSlotCount(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            int durationInMinute
    ) {
        int minutesNumber = Math.toIntExact(startInclusive.until(endExclusive, ChronoUnit.MINUTES));
        int slot = minutesNumber / durationInMinute;
        slot = slot + (
                minutesNumber % durationInMinute > 0
                        ? 1
                        : 0
        );
        return slot;
    }

    public SchedulingDateTimeSlot calcDelayDateTimeSlot(
            TreeSet<SchedulingDateTimeSlot> valueRange,
            int delay
    ) {
        return valueRange.stream()
                .skip(delay)
                .findFirst()
                .get();
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
