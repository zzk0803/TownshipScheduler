package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.pojo.form.BillScheduleRequest;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ITownshipSchedulingService {

    UUID prepareScheduling(BillScheduleRequest billScheduleRequest);

    CompletableFuture<Void> scheduling(UUID problemId);

    CompletableFuture<Void> abort(UUID problemId);

    TownshipSchedulingProblem getSchedule(UUID problemId);

    boolean checkUuidIsValidForSchedule(String uuid);

}
