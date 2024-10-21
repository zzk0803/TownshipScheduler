package zzk.townshipscheduler.adopting.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zzk.townshipscheduler.backend.persistence.Goods;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class BillItem {

    private int serial;

    private Goods goods;

    private int amount = 1;

}
