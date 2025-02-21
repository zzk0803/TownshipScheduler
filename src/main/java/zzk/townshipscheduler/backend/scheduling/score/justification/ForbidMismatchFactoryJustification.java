package zzk.townshipscheduler.backend.scheduling.score.justification;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInfo;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryTimeSlotInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;

public class ForbidMismatchFactoryJustification implements ConstraintJustification {

    private final String readable;

    public ForbidMismatchFactoryJustification(SchedulingPlayerFactoryAction action, BendableScore bendableScore) {
        SchedulingFactoryTimeSlotInstance planningFactory = action.getPlanningTimeSlotFactory();
        SchedulingFactoryInfo requireFactory = action.getSchedulingProduct().getRequireFactory();
        SchedulingFactoryInfo planningFactoryInfo = planningFactory.getFactoryInstance().getSchedulingFactoryInfo();
        this.readable = "action {%d},producing {%s},require factory type is {%s},scheduler try {%s} "
                .formatted(action.getActionId(), action.getSchedulingProduct(), requireFactory, planningFactoryInfo);
    }

}
