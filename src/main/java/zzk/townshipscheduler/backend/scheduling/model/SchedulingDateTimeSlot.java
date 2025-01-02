package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@PlanningEntity
public class SchedulingDateTimeSlot {

    private static ThreadLocal<Set<SchedulingDateTimeSlot>> cached = new ThreadLocal<>();

    @PlanningId
    private Integer id;

    private LocalDateTime start;

    private LocalDateTime end;

    private int durationInMinute;

    @InverseRelationShadowVariable(sourceVariableName = "planningDateTimeSlot")
    private List<SchedulingGameAction> assignedActionInTimeSlot;

    public static SchedulingDateTimeSlot of(
            final LocalDateTime dateTime
    ) {
        Set<SchedulingDateTimeSlot> dateTimeSlots = cached.get();
        return dateTimeSlots.stream().filter(slot -> {
            LocalDateTime slotStart = slot.getStart();
            LocalDateTime slotEnd = slot.getEnd();
            return (slotStart.isAfter(dateTime) || slotStart.isEqual(dateTime)) && slotEnd.isBefore(dateTime);
        }).findFirst().get();
    }

    public static Set<SchedulingDateTimeSlot> generate(
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

}
