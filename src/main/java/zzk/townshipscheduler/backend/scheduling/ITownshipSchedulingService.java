package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ITownshipSchedulingService {

    TownshipSchedulingProblem prepareScheduling(TownshipSchedulingRequest townshipSchedulingRequest);

    void scheduling(UUID problemId);

    void scheduling(UUID problemId, Consumer<TownshipSchedulingProblem> problemConsumer);

    void scheduling(
            UUID problemId,
            Consumer<TownshipSchedulingProblem> problemConsumer,
            BiConsumer<UUID, Throwable> solveExceptionConsumer
    );

    void abort(UUID problemId);

    TownshipSchedulingProblem getSchedule(UUID problemId);

    boolean checkUuidIsValidForSchedule(String uuid);

}
