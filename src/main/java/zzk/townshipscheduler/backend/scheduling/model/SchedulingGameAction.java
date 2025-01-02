package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


@Data
@NoArgsConstructor
@PlanningEntity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class SchedulingGameAction {

    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PRIVATE)
    @EqualsAndHashCode.Include
    @PlanningId
    protected Integer id;

    @EqualsAndHashCode.Include
    protected String uuid = UUID.randomUUID().toString();

    protected SchedulingGameActionObject schedulingGameActionObject;

    protected SchedulingGameAction sourceGameAction;

    protected SchedulingGameAction sinkGameAction;

    protected List<SchedulingGameAction> prerequisiteActions;

    protected List<SchedulingGameAction> succeedingActions;

    @PlanningVariable
    protected SchedulingDateTimeSlot planningDateTimeSlot;

    private Flag flag = Flag.SCHEDULING;

    public SchedulingGameAction(SchedulingGameActionObject schedulingGameActionObject) {
        this.schedulingGameActionObject = schedulingGameActionObject;
    }

    public static GameActionIdRoller createIdRoller() {
        return new GameActionIdRoller();
    }

    public static SchedulingGameAction createSourceAction() {
        return new SchedulingGameActionDummy(Flag.SOURCE);
    }

    public static SchedulingGameAction createSinkAction() {
        return new SchedulingGameActionDummy(Flag.SINK);
    }

    public void idRoller(GameActionIdRoller idRoller) {
        idRoller.setup(this);
    }

    public void biAssociateWholeToPart(SchedulingGameAction partAction) {
        this.appendPrerequisiteAction(partAction);
        partAction.appendSucceedingAction(this);
    }

    private void appendPrerequisiteAction(SchedulingGameAction schedulingGameAction) {
        if (this.prerequisiteActions == null) {
            this.prerequisiteActions = new ArrayList<>();
        }
        this.prerequisiteActions.add(schedulingGameAction);
    }

    private void appendSucceedingAction(SchedulingGameAction schedulingGameAction) {
        if (this.succeedingActions == null) {
            this.succeedingActions = new ArrayList<>();
        }
        this.succeedingActions.add(schedulingGameAction);
    }

    public abstract String getHumanReadable();

    public void readyElseThrow() {
        Objects.requireNonNull(getId());
    }

    public static enum Flag {
        SOURCE,
        SCHEDULING,
        SINK
    }

    public static class GameActionIdRoller {

        private AtomicInteger idRoller;

        private GameActionIdRoller() {
            idRoller = new AtomicInteger(1);
        }

        public void setup(SchedulingGameAction schedulingGameAction) {
            schedulingGameAction.setId(idRoller.getAndIncrement());
        }

    }

}
