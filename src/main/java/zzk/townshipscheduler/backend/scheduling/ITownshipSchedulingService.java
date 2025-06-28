package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.lang.String;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ITownshipSchedulingService {

    @NonNull
    ScoreAnalysis<BendableScore> analyze(@NonNull TownshipSchedulingProblem townshipSchedulingProblem);

    @NonNull
    ScoreExplanation<TownshipSchedulingProblem, BendableScore> explain(
            @NonNull TownshipSchedulingProblem townshipSchedulingProblem
    );

    TownshipSchedulingProblem prepareScheduling(TownshipSchedulingRequest townshipSchedulingRequest);

    void scheduling(String problemId);

    void scheduling(String problemId, Consumer<TownshipSchedulingProblem> problemConsumer);

    void scheduling(
            String problemId,
            Consumer<TownshipSchedulingProblem> problemConsumer,
            BiConsumer<String, Throwable> solveExceptionConsumer
    );

    void abort(String problemId);

    TownshipSchedulingProblem getSchedule(String problemId);

    boolean checkUuidIsValidForSchedule(String uuid);

}
