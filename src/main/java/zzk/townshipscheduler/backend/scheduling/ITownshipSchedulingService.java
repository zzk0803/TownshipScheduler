package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

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

    String getProblemSizeStatistics(String problemId);

    @NonNull ScoreAnalysis<BendableScore> analyze(
            @NonNull TownshipSchedulingProblem townshipSchedulingProblem
    );

    @NonNull ScoreExplanation<TownshipSchedulingProblem, BendableScore> explain(
            @NonNull TownshipSchedulingProblem townshipSchedulingProblem
    );

    boolean checkWeatherReadyToSolve(String uuid);

    void remove(String problemId);

    void persist(TownshipSchedulingProblem townshipSchedulingProblem);

    void persist(String problemId);

    Optional<TownshipSchedulingProblem> load(String problemId);

}
