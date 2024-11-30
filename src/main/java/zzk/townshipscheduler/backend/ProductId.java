package zzk.townshipscheduler.backend;

import java.util.Objects;

public final class ProductId {

    private final Long value;

    private ProductId(Long value) {
        this.value = value;
    }

    public static ProductId of(Long value) {
        return new ProductId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductId productId)) return false;

        return Objects.equals(getValue(), productId.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }

}
