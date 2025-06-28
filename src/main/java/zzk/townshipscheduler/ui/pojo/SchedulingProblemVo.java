package zzk.townshipscheduler.ui.pojo;

import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;

import java.util.List;

@Data
public class SchedulingProblemVo {

    private String uuid;

    private SolverStatus solverStatus;

    private List<SchedulingOrder> orderList;

}
