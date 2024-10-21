package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.adopting.form.BillScheduleRequest;

import java.util.UUID;

public interface ITownshipSchedulingService {

    UUID prepareScheduling(BillScheduleRequest billScheduleRequest);

}
