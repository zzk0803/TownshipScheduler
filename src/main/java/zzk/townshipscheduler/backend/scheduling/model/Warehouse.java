package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import zzk.townshipscheduler.backend.persistence.Goods;

import java.util.Map;

@Data
public class Warehouse {

    private Long id;

    private SchedulingPlayer belongingPlayer;

    private Integer warehouseCapacity;

    private Map<Goods, Integer> warehouseStockGoodsAmountMap;

}
