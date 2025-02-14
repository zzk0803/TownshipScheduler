package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class TownshipSchedulingServiceImpl implements ITownshipSchedulingService {

    private final SolverManager<TownshipSchedulingProblem, UUID> solverManager;

    private final SolutionManager<TownshipSchedulingProblem, HardMediumSoftLongScore> solutionManager;

    private final Map<UUID, TownshipSchedulingProblem> idProblemMap = new ConcurrentHashMap<>();

    private final Consumer<TownshipSchedulingProblem> defaultConsumer
            = townshipSchedulingProblem -> {
        UUID uuid = townshipSchedulingProblem.getUuid();
        idProblemMap.put(uuid, townshipSchedulingProblem);
    };

    private final BiConsumer<UUID, Throwable> defaultExceptionHandler
            = (uuid, throwable) -> {
        log.error("problem {} exception {}", uuid, throwable);
    };

    private final Map<UUID, SolverJob<TownshipSchedulingProblem, UUID>> idSolverJobMap = new ConcurrentHashMap<>();

    @Override
    public TownshipSchedulingProblem prepareScheduling(TownshipSchedulingRequest townshipSchedulingRequest) {
        MappingProcess process = new MappingProcess(townshipSchedulingRequest);
        TownshipSchedulingProblem townshipSchedulingProblem = process.map();
        UUID uuid = townshipSchedulingProblem.getUuid();
        idProblemMap.put(uuid, townshipSchedulingProblem);
        return townshipSchedulingProblem;
    }

    @Override
    public void scheduling(UUID problemId) {
        SolverJob<TownshipSchedulingProblem, UUID> solverJob
                = solverManager.solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(this::getSchedule)
                .withBestSolutionConsumer(defaultConsumer)
                .withExceptionHandler(defaultExceptionHandler)
                .run();
        idSolverJobMap.put(problemId, solverJob);
    }

    @Override
    public void scheduling(UUID problemId, Consumer<TownshipSchedulingProblem> problemConsumer) {
        SolverJob<TownshipSchedulingProblem, UUID> solverJob
                = solverManager.solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(this::getSchedule)
                .withBestSolutionConsumer(
                        defaultConsumer.andThen(problemConsumer)
                )
                .run();
        idSolverJobMap.put(problemId, solverJob);
    }

    @Override
    public void scheduling(
            UUID problemId,
            Consumer<TownshipSchedulingProblem> problemConsumer,
            BiConsumer<UUID, Throwable> solveExceptionConsumer
    ) {
        SolverJob<TownshipSchedulingProblem, UUID> solverJob
                = solverManager.solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(this::getSchedule)
                .withBestSolutionConsumer(
                        defaultConsumer.andThen(problemConsumer)
                )
                .withExceptionHandler(defaultExceptionHandler.andThen(solveExceptionConsumer))
                .run();
        idSolverJobMap.put(problemId, solverJob);
    }

    @Override
    public void abort(UUID problemId) {
        solverManager.terminateEarly(problemId);
    }

    @Override
    public TownshipSchedulingProblem getSchedule(UUID problemId) {
        SolverStatus solverStatus = solverManager.getSolverStatus(problemId);
        TownshipSchedulingProblem townshipSchedulingProblem = idProblemMap.get(problemId);
        townshipSchedulingProblem.setSolverStatus(solverStatus);
        return townshipSchedulingProblem;
    }

    @Override
    public boolean checkUuidIsValidForSchedule(String uuid) {
        if (Objects.isNull(uuid) || uuid.isBlank()) {
            return false;
        }

        return idProblemMap.containsKey(UUID.fromString(uuid));
    }

}
