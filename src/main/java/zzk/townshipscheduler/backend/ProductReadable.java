package zzk.townshipscheduler.backend;

import lombok.Getter;

public final class ProductReadable {

    @Getter
    private final String content;

    private ProductReadable(String content) {
        this.content = content;
    }

    public static ProductReadable of(String content) {
        return new ProductReadable(content);
    }

}
