package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

@Data
@EqualsAndHashCode(callSuper = true)
public class SchedulingGameActionOrderFulfill extends SchedulingGameAction {

    public SchedulingGameActionOrderFulfill(SchedulingGameActionObject schedulingGameActionObject) {
        super(schedulingGameActionObject);
    }

    @Override
    public String getHumanReadable() {
        SchedulingGameActionObject gameActionObject = this.getSchedulingGameActionObject();
        Assert.isInstanceOf(SchedulingOrder.class, gameActionObject);
        SchedulingOrder schedulingOrder = (SchedulingOrder) gameActionObject;
        return "Fulfill::" + schedulingOrder.getProductAmountBill().toString();
    }

}
