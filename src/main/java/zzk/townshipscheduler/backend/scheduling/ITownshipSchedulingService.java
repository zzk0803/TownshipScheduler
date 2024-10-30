package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.port.form.BillScheduleRequest;

import java.util.UUID;

public interface ITownshipSchedulingService {

    UUID prepareScheduling(BillScheduleRequest billScheduleRequest);

    boolean checkUuidIsValidForSchedule(String uuid);

}
