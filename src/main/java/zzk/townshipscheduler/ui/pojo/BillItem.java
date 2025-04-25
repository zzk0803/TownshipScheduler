package zzk.townshipscheduler.ui.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class BillItem {

    private int serial;

    private ProductEntity productEntity;

    private int amount = 1;

}
