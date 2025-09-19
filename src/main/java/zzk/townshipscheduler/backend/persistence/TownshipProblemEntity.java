package zzk.townshipscheduler.backend.persistence;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import jakarta.persistence.Converter;
import lombok.Data;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Data
public class TownshipProblemEntity {

    private String uuid;

    private Set<TownshipArrangementEntity> townshipArrangementEntitySet;

    private LocalDateTime workCalendarStart;

    private LocalDateTime workCalendarEnd;

    private int dateTimeSlotSizeInMinute;

    private Long playerId;

    private PlayerEntity player;

    private Set<Long> orderIdSet;

    private Set<OrderEntity> orderEntitySet;

    private LocalTime sleepStartPickerValue;

    private LocalTime sleepEndPickerValue;

    public TownshipProblemEntity(TownshipSchedulingProblem townshipSchedulingProblem) {
       townshipSchedulingProblem.getUuid();
        townshipSchedulingProblem.getSchedulingWorkCalendar();
        townshipSchedulingProblem.getDateTimeSlotSize();
        townshipSchedulingProblem.getSchedulingOrderList();
        townshipSchedulingProblem.getSchedulingPlayer();
    }

}
