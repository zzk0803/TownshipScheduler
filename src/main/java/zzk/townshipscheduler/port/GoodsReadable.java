package zzk.townshipscheduler.port;

import lombok.Getter;

public final class GoodsReadable {

    @Getter
    private final String content;

    private GoodsReadable(String content) {
        this.content = content;
    }

    public static GoodsReadable of(String content) {
        return new GoodsReadable(content);
    }

}
