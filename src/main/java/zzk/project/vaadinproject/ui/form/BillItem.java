package zzk.project.vaadinproject.ui.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zzk.project.vaadinproject.backend.persistence.Goods;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class BillItem {

    private int serial;

    private Goods goods;

    private int amount = 1;

}
