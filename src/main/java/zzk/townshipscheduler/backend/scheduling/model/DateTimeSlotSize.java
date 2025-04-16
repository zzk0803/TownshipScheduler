package zzk.townshipscheduler.backend.scheduling.model;

public enum DateTimeSlotSize {
    SMALL(10),
    HALF_HOUR(30),
    HOUR(60),
    BIG(180);

    private final int minute;

    DateTimeSlotSize(int minute) {
        this.minute = minute;
    }

    public int getMinute() {
        return minute;
    }
}
