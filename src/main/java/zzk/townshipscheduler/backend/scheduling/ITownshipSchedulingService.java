package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.lang.String;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ITownshipSchedulingService {

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
