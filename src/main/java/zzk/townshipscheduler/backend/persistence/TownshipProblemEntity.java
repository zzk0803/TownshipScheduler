package zzk.townshipscheduler.backend.persistence;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.util.Set;

public class TownshipProblemEntity {

    private String uuid;

    private Set<TownshipArrangementEntity> townshipArrangementEntitySet;

    private int dateTimeSlotSizeInMinute;

    private LocalDateTime workCalendarStart;

    private LocalDateTime workCalendarEnd;

    private Integer dataTimeSlotId;

    private PlayerEntity player;

    private Set<OrderEntity> orderEntitySet;

}
