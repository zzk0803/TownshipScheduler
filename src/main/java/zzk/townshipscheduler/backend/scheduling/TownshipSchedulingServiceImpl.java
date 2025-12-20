package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.dao.TownshipProblemEntityRepository;
import zzk.townshipscheduler.backend.persistence.TownshipProblemEntity;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.ui.pojo.SchedulingProblemVo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TownshipSchedulingServiceImpl implements ITownshipSchedulingService {

    private final SolverManager<TownshipSchedulingProblem, String> solverManager;

    private final SolutionManager<TownshipSchedulingProblem, BendableScore> solutionManager;

    private final TownshipProblemEntityRepository townshipProblemEntityRepository;

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
    public boolean existSolvingJob(String problemId) {
        return this.idSolverJobMap.containsKey(problemId);
    }

    @Override
    public boolean existProblem(String problemId) {
        return this.idProblemMap.containsKey(problemId) || this.townshipProblemEntityRepository.existsById(problemId);
    }

    @Override
    public TownshipSchedulingProblem prepareScheduling(TownshipSchedulingRequest townshipSchedulingRequest) {
        ProblemTransferProcess process = new ProblemTransferProcess(townshipSchedulingRequest);
        TownshipSchedulingProblem townshipSchedulingProblem = process.buildProblem();
        var uuid = townshipSchedulingProblem.getUuid();
        idProblemMap.put(uuid, townshipSchedulingProblem);
        return townshipSchedulingProblem;
    }

    @Override
    public void scheduling(
            String problemId,
            Consumer<TownshipSchedulingProblem> solverJobStartedEventConsumer,
            Consumer<TownshipSchedulingProblem> bestSolutionEventConsumer,
            Consumer<TownshipSchedulingProblem> finalBestSolutionEventConsumer,
            BiConsumer<String, Throwable> exceptionHandler
    ) {
        SolverJob<TownshipSchedulingProblem, String> solverJob = solverManager.solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(this::getSchedule)
                .withSolverJobStartedEventConsumer(
                        solverJobStartedEvent -> {
                            TownshipSchedulingProblem solution = solverJobStartedEvent.solution();
                            solverJobStartedEventConsumer.accept(solution);
                        })
                .withBestSolutionEventConsumer(
                        solutionNewBestSolutionEvent -> {
                            TownshipSchedulingProblem solution = solutionNewBestSolutionEvent.solution();
                            defaultConsumer.andThen(bestSolutionEventConsumer)
                                    .accept(solution);
                        })
                .withFinalBestSolutionEventConsumer(
                        finalBestSolutionEvent -> {
                            defaultConsumer.andThen(finalBestSolutionEventConsumer)
                                    .andThen(this::persist)
                                    .accept(finalBestSolutionEvent.solution())
                            ;
                        })
                .withExceptionHandler(defaultExceptionHandler.andThen(exceptionHandler))
                .run()
                ;
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
    public String getProblemSizeStatistics(String problemId) {
        SolverJob<TownshipSchedulingProblem, String> solverJob = this.idSolverJobMap.get(problemId);
        if (solverJob == null) {
            return "";
        } else {
            return solverJob.getProblemSizeStatistics()
                    .approximateProblemScaleAsFormattedString();
        }
    }

    @Override
    public @NonNull ScoreAnalysis<BendableScore> analyze(
            @NonNull TownshipSchedulingProblem townshipSchedulingProblem
    ) {
        return solutionManager.analyze(townshipSchedulingProblem);
    }

    @Override
    public @NonNull ScoreExplanation<TownshipSchedulingProblem, BendableScore> explain(
            @NonNull TownshipSchedulingProblem townshipSchedulingProblem
    ) {
        return solutionManager.explain(townshipSchedulingProblem);
    }

    @Override
    public boolean checkWeatherReadyToSolve(String uuid) {
        if (Objects.isNull(uuid) || uuid.isBlank()) {
            return false;
        }

        return idProblemMap.containsKey(uuid);
    }

    @Override
    public void remove(String problemId) {
        SolverJob<TownshipSchedulingProblem, String> solverJob = this.idSolverJobMap.get(problemId);
        if (Objects.nonNull(solverJob)) {
            solverJob.terminateEarly();
            this.idSolverJobMap.remove(problemId, solverJob);
        }
        if (townshipProblemEntityRepository.existsById(problemId)) {
            townshipProblemEntityRepository.deleteById(problemId);
        }
        this.idProblemMap.remove(problemId);
    }

    @Override
    public void persist(TownshipSchedulingProblem townshipSchedulingProblem) {
        ProblemPersistingPrecess problemPersistingPrecess
                = new ProblemPersistingPrecess(townshipSchedulingProblem);
        TownshipProblemEntity townshipProblemEntity = problemPersistingPrecess.process();
        townshipProblemEntityRepository.save(townshipProblemEntity);
    }

    @Override
    public void persist(String problemId) {
        TownshipSchedulingProblem townshipSchedulingProblem = getSchedule(problemId);
        persist(townshipSchedulingProblem);
    }

    @Override
    public Optional<TownshipSchedulingProblem> load(String problemId) {
        if (this.townshipProblemEntityRepository.existsById(problemId)) {
            Optional<TownshipProblemEntity> townshipProblemEntityOptional
                    = this.townshipProblemEntityRepository.findByUuid(problemId, TownshipProblemEntity.class);
            return townshipProblemEntityOptional.map(
                            townshipProblemEntity -> {
                                return new ProblemExternalizedProcess(townshipProblemEntity.getProblemSerialized());
                            }
                    )
                    .map(ProblemExternalizedProcess::process)
                    .map(townshipSchedulingProblem -> this.idProblemMap.putIfAbsent(problemId, townshipSchedulingProblem))
                    ;
        } else {
            return Optional.empty();
        }

    }

    public Collection<SchedulingProblemVo> allReadySchedulingProblem() {
        return this.idProblemMap.entrySet()
                .stream()
                .map(entry -> {
                    SchedulingProblemVo schedulingProblemVo = new SchedulingProblemVo();
                    schedulingProblemVo.setUuid(entry.getKey());
                    schedulingProblemVo.setSolverStatus(solverManager.getSolverStatus(entry.getKey()));
                    TownshipSchedulingProblem townshipSchedulingProblem = entry.getValue();
                    List<SchedulingOrder> orderList = townshipSchedulingProblem.getSchedulingOrderList();
                    schedulingProblemVo.setOrderList(orderList);
                    return schedulingProblemVo;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

}
