package zzk.townshipscheduler.backend.scheduling.algorithm;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;

import java.util.Comparator;

public class SchedulingDateTimeStrengthComparator
        implements Comparator<SchedulingDateTimeSlot> {

    @Override
    public int compare(
            SchedulingDateTimeSlot o1,
            SchedulingDateTimeSlot o2
    ) {
        return SchedulingDateTimeSlot.DATE_TIME_SLOT_COMPARATOR.compare(
                o1,
                o2
        );
    }

}
