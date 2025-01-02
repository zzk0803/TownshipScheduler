package zzk.townshipscheduler.backend.scheduling.model;

import java.util.List;
import java.util.Set;

public sealed interface SchedulingGameActionObject permits SchedulingOrder,SchedulingProduct {

    List<SchedulingGameAction> getGameActionSet();

}
