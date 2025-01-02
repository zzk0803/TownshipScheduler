package zzk.townshipscheduler.backend.persistence;

public enum OrderEntityScheduleState {
    NONE(0, "Just Create"),
    HAS_ARRANGE(1, "Arranged Schedule,Not Finish"),
    HAS_SCHEDULE(2, "Schedule Complete"),
    ;

    private final int code;

    private final String string;

    OrderEntityScheduleState(int code, String string) {
        this.code = code;
        this.string = string;
    }
}
