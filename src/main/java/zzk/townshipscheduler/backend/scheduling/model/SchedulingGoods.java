package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.port.GoodId;
import zzk.townshipscheduler.port.GoodsHierarchy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Data
public class SchedulingGoods {

    private GoodId goodId;

    private String name;

    private String category;

    private Integer level;

    private Integer cost;

    private Integer sellPrice;

    private Integer xp;

    private Integer dealerValue;

    private Integer helpValue;

    private boolean atomicGoods;

    private String bomString;

    private transient GoodsHierarchy goodsHierarchy;

    private transient Map<SchedulingGoods, Integer> bom;

    private String durationString;

    private transient Duration producingDuration;

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
        this.bomString = goods.getBomString();
        this.durationString = goods.getDurationString();
    }

}
