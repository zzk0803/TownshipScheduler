package zzk.townshipscheduler.port;

public final class GoodId {

    private final Long value;

    private GoodId(Long value) {
        this.value = value;
    }

    public static GoodId of(Long value) {
        return new GoodId(value);
    }

    public Long getValue() {
        return value;
    }

}
