package zzk.townshipscheduler.backend.scheduling.model.fact;

import lombok.Data;
import zzk.townshipscheduler.port.GoodId;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.backend.scheduling.model.WorkObject;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

@Data
public class SchedulingGoods implements WorkObject {

    private GoodId goodId;

    private String name;

    private String category;

    private Integer level;

    private Integer cost;

    private Integer sellPrice;

    private Integer xp;

    private Integer dealerValue;

    private Integer helpValue;

    public SchedulingGoods(Goods goods) {
        this.goodId = GoodId.of(goods.getId());
        this.name = goods.getName();
        this.category = goods.getCategory();
        this.level = goods.getLevel();
        this.cost = goods.getCost();
        this.sellPrice = goods.getSellPrice();
        this.xp = goods.getXp();
        this.dealerValue = goods.getDealerValue();
        this.helpValue = goods.getHelpValue();
    }

}
