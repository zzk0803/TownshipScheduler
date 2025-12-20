package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ITownshipSchedulingService {

    boolean existSolvingJob(String problemId);

    boolean existProblem(String problemId);

    TownshipSchedulingProblem prepareScheduling(TownshipSchedulingRequest townshipSchedulingRequest);

    void scheduling(
            String problemId,
            Consumer<TownshipSchedulingProblem> solverJobStartedEventConsumer,
            Consumer<TownshipSchedulingProblem> bestSolutionEventConsumer,
            Consumer<TownshipSchedulingProblem> finalBestSolutionEventConsumer,
            BiConsumer<String, Throwable> exceptionHandler
    );

    void abort(String problemId);

    TownshipSchedulingProblem getSchedule(String problemId);

    SolverStatus getProblemSolverStatus(String problemId);

    String getProblemSizeStatistics(String problemId);

    @NonNull ScoreAnalysis<BendableScore> analyze(
            @NonNull TownshipSchedulingProblem townshipSchedulingProblem
    );

    @NonNull ScoreExplanation<TownshipSchedulingProblem, BendableScore> explain(
            @NonNull TownshipSchedulingProblem townshipSchedulingProblem
    );

    boolean checkWeatherReadyToSolve(String uuid);

    void unlink(String problemId);

    void remove(String problemId);

    void persist(TownshipSchedulingProblem townshipSchedulingProblem);

    void persist(String problemId);

    Optional<TownshipSchedulingProblem> load(String problemId);

    Collection<TownshipSchedulingProblem> loadPersistedSchedulingProblem();

    Collection<TownshipSchedulingProblem> getLinkedSchedulingProblem();

}
