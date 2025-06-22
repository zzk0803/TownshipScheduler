package zzk.townshipscheduler.ui.pojo;

import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;

@Data
public class SchedulingProblemVo {

    private String uuid;

    private SolverStatus solverStatus;

}
