package zzk.townshipscheduler.backend.scheduling.model;

public enum DateTimeSlotSize {
    TEN_MINUTES(10),
    HALF_HOUR(30),
    HOUR(60),
    TWO_HOUR(120),
    THREE_HOUR(180);

    private final int minute;

    DateTimeSlotSize(int minute) {
        this.minute = minute;
    }

    public int getMinute() {
        return minute;
    }
}
