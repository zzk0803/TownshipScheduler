package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.ui.pojo.SchedulingProblemVo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TownshipSchedulingServiceImpl implements ITownshipSchedulingService {

    private final SolverManager<TownshipSchedulingProblem, String> solverManager;

    private final Map<String, TownshipSchedulingProblem> idProblemMap = new ConcurrentHashMap<>();

    private final Consumer<TownshipSchedulingProblem> defaultConsumer
            = townshipSchedulingProblem -> {
        var uuid = townshipSchedulingProblem.getUuid();
        idProblemMap.put(uuid, townshipSchedulingProblem);
    };

    private final BiConsumer<String, Throwable> defaultExceptionHandler
            = (uuid, throwable) -> {
        log.error("problem {} exception {}", uuid, throwable);
    };

    private final Map<String, SolverJob<TownshipSchedulingProblem, String>> idSolverJobMap = new ConcurrentHashMap<>();

    @Override
    public TownshipSchedulingProblem prepareScheduling(TownshipSchedulingRequest townshipSchedulingRequest) {
        EntityProblemTransferProcess process = new EntityProblemTransferProcess(townshipSchedulingRequest);
        TownshipSchedulingProblem townshipSchedulingProblem = process.buildProblem();
        var uuid = townshipSchedulingProblem.getUuid();
        idProblemMap.put(uuid, townshipSchedulingProblem);
        return townshipSchedulingProblem;
    }

    @Override
    public void scheduling(String problemId) {
        SolverJob<TownshipSchedulingProblem, String> solverJob
                = solverManager.solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(this::getSchedule)
                .withBestSolutionConsumer(defaultConsumer)
                .withExceptionHandler(defaultExceptionHandler)
                .run();
        idSolverJobMap.put(problemId, solverJob);
    }

    @Override
    public void scheduling(String problemId, Consumer<TownshipSchedulingProblem> problemConsumer) {
        SolverJob<TownshipSchedulingProblem, String> solverJob
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
            String problemId,
            Consumer<TownshipSchedulingProblem> problemConsumer,
            BiConsumer<String, Throwable> solveExceptionConsumer
    ) {
        SolverJob<TownshipSchedulingProblem, String> solverJob
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
    public void abort(String problemId) {
        if (Objects.isNull(problemId) || problemId.isBlank()) {
            return;
        }

        solverManager.terminateEarly(problemId);
    }

    @Override
    public TownshipSchedulingProblem getSchedule(String problemId) {
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

        return idProblemMap.containsKey(uuid);
    }

    public void schedulingWithSolverManager(
            Function<
                    SolverManager<TownshipSchedulingProblem, String>,
                    SolverJob<TownshipSchedulingProblem, String>
            > solverManagerConsumer
    ) {
        SolverJob<TownshipSchedulingProblem, String> solverJob
                = solverManagerConsumer.apply(this.solverManager);
        String problemId = solverJob.getProblemId();
        idSolverJobMap.put(problemId, solverJob);
    }

    public Collection<SchedulingProblemVo> allSchedulingProblem() {
       return this.idProblemMap.entrySet()
                .stream()
                .map(entry -> {
                    SchedulingProblemVo schedulingProblemVo = new SchedulingProblemVo();
                    schedulingProblemVo.setUuid(entry.getKey());
                    schedulingProblemVo.setSolverStatus(solverManager.getSolverStatus(entry.getKey()));
                    return schedulingProblemVo;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
