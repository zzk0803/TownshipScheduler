package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

@Data
@EqualsAndHashCode(callSuper = true)
public class SchedulingGameActionProductStocking extends SchedulingGameAction{

    public SchedulingGameActionProductStocking(SchedulingGameActionObject schedulingGameActionObject) {
        super(schedulingGameActionObject);
    }

    @Override
    public String getHumanReadable() {
        SchedulingGameActionObject gameActionObject = this.getSchedulingGameActionObject();
        Assert.isInstanceOf(SchedulingProduct.class, gameActionObject);
        SchedulingProduct schedulingProduct = (SchedulingProduct) gameActionObject;
        return "Reap And Stock::" + schedulingProduct.getName();
    }

}
