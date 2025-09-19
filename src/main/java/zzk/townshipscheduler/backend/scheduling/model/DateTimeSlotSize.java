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

    public static DateTimeSlotSize of(int minute) {
        return switch (minute) {
            case 10 -> TEN_MINUTES;
            case 30 -> HALF_HOUR;
            case 60 -> HOUR;
            case 120 -> TWO_HOUR;
            case 180 -> THREE_HOUR;
            default -> throw new IllegalStateException("Unexpected value: " + minute);
        };
    }
}
