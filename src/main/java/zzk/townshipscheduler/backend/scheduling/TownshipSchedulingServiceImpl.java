package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TownshipSchedulingServiceImpl implements ITownshipSchedulingService {

    public static final int SCHEDULING_END_DAY_FROM_START = 2;

    private final ExecutorService executorService;

    private final TownshipSchedulingProblemHolder problemHolder;

    private final SolverManager<TownshipSchedulingProblem, UUID> solverManager;

    private final SolutionManager<TownshipSchedulingProblem, HardMediumSoftLongScore> solutionManager;

    @Override
    public TownshipSchedulingProblem prepareScheduling(TownshipSchedulingRequest townshipSchedulingRequest) {
        MappingProcess process = new MappingProcess(townshipSchedulingRequest);
        TownshipSchedulingProblem townshipSchedulingProblem = process.map();
        problemHolder.write(townshipSchedulingProblem);
        return townshipSchedulingProblem;
    }

    @Override
    public CompletableFuture<Void> scheduling(UUID problemId) {
        return CompletableFuture.runAsync(
                () -> {
                    SolverJobBuilder<TownshipSchedulingProblem, UUID> solverJobBuilder = solverManager.solveBuilder();
                    SolverJob<TownshipSchedulingProblem, UUID> solverJob = solverJobBuilder
                            .withProblemId(problemId)
                            .withProblemFinder(this::getSchedule)
                            .withBestSolutionConsumer(problemHolder::write)
                            .run();
                }, executorService
        );
    }

    @Override
    public void abort(UUID problemId) {
        CompletableFuture.runAsync(
                () -> {
                    solverManager.terminateEarly(problemId);
                }, executorService
        );
    }

    @Override
    public TownshipSchedulingProblem getSchedule(UUID problemId) {
        SolverStatus solverStatus = solverManager.getSolverStatus(problemId);
        TownshipSchedulingProblem townshipSchedulingProblem = problemHolder.read();
        townshipSchedulingProblem.setSolverStatus(solverStatus);
        return townshipSchedulingProblem;
    }

    @Override
    public boolean checkUuidIsValidForSchedule(String uuid) {
        if (Objects.isNull(uuid) || uuid.isBlank()) {
            return false;
        }

        TownshipSchedulingProblem problem = problemHolder.read();
        if (Objects.isNull(problem)) {
            return false;
        }
        return problem.getUuid().toString().equals(uuid);
    }

}
