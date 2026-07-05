package zzk.townshipscheduler.ui.pojo;

import zzk.townshipscheduler.backend.persistence.ProductEntity;

public record BillItem(
        int serial,
        ProductEntity productEntity,
        int amount
) {

    public BillItem{
        if (amount < 0) {
            throw new IllegalArgumentException("amount never be negative");
        }
    }

    public BillItem update(int amount) {
        return new BillItem(this.serial, productEntity, amount);
    }

}
