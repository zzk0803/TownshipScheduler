package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ITownshipSchedulingService {

    TownshipSchedulingProblem prepareScheduling(TownshipSchedulingRequest townshipSchedulingRequest);

    CompletableFuture<Void> scheduling(UUID problemId);

    void abort(UUID problemId);

    TownshipSchedulingProblem getSchedule(UUID problemId);

    boolean checkUuidIsValidForSchedule(String uuid);

}
