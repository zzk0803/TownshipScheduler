package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverStatus;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ITownshipSchedulingService {

    TownshipSchedulingProblem prepareScheduling(TownshipSchedulingRequest townshipSchedulingRequest);

    boolean existSolvingJob(String problemId);

    boolean existProblem(String problemId);

    SolverStatus getProblemSolverStatus(String problemId);

    String getProblemSizeStatistics(String problemId);

    TownshipSchedulingProblem getProblem(String problemId);

    TownshipSchedulingProblem gatherProblem(String problemId);

    SolverJob<TownshipSchedulingProblem> scheduling(
            String problemId,
            Consumer<TownshipSchedulingProblem> solverJobStartedEventConsumer,
            Consumer<TownshipSchedulingProblem> bestSolutionEventConsumer,
            Consumer<TownshipSchedulingProblem> finalBestSolutionEventConsumer,
            BiConsumer<Object, Throwable> exceptionHandler
    );

    void abort(String problemId);

     CompletableFuture<File> benchmark(TownshipSchedulingBenchmarkRequest benchmarkRequest);

//    @NonNull ScoreAnalysis<HardMediumSoftBigDecimalScore> analyze(
//            @NonNull TownshipSchedulingProblem townshipSchedulingProblem
//    );
//
//    @NonNull ScoreAnalysis<HardMediumSoftBigDecimalScore> explain(
//            @NonNull TownshipSchedulingProblem townshipSchedulingProblem
//    );

    boolean checkWeatherReadyToSolve(String uuid);

    void unlink(String problemId);

    void remove(String problemId);

    void persist(TownshipSchedulingProblem townshipSchedulingProblem);

    void persist(String problemId);

    Optional<TownshipSchedulingProblem> load(String problemId);

    Collection<TownshipSchedulingProblem> loadPersistedSchedulingProblem();

    Collection<TownshipSchedulingProblem> getLinkedSchedulingProblem();

}
