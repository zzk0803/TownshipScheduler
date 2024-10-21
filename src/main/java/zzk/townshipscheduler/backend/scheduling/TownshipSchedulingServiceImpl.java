package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.adopting.form.BillScheduleRequest;
import zzk.townshipscheduler.backend.persistence.Bill;
import zzk.townshipscheduler.backend.scheduling.mapping.BillMapper;
import zzk.townshipscheduler.backend.tfdemo.PackagingSchedule;

import java.util.List;
import java.util.UUID;

@Service(value = "schedulingService")
@RequiredArgsConstructor
public class TownshipSchedulingServiceImpl implements ITownshipSchedulingService {

    private final BillMapper billMapper;

    private final SolverManager<PackagingSchedule, String> solverManager;

    private final SolutionManager<PackagingSchedule, HardMediumSoftLongScore> solutionManager;

    @Override
    public UUID prepareScheduling(BillScheduleRequest billScheduleRequest) {
        List<Bill> bills = billScheduleRequest.getBills();

        return null;
    }

}
