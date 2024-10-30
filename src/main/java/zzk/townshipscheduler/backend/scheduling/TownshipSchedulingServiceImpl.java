package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.service.GoodsService;
import zzk.townshipscheduler.backend.tfdemo.foodpacking.PackagingSchedule;
import zzk.townshipscheduler.port.form.BillScheduleRequest;

import java.util.UUID;

@Service(value = "schedulingService")
@RequiredArgsConstructor
public class TownshipSchedulingServiceImpl implements ITownshipSchedulingService {

    private final GoodsService goodsService;

    private final SolverManager<PackagingSchedule, String> solverManager;

    private final SolutionManager<PackagingSchedule, HardMediumSoftLongScore> solutionManager;

    @Override
    public UUID prepareScheduling(BillScheduleRequest billScheduleRequest) {
        return UUID.randomUUID();
    }

    @Override
    public boolean checkUuidIsValidForSchedule(String uuid) {
        return false;
    }


}
