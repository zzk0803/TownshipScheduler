package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import zzk.townshipscheduler.backend.persistence.GoodsHierarchy;

import java.time.Duration;

@Data
public class SchedulingGoods {

    private Long id;

    private String name;

    private String category;

    private Integer level;

    private GoodsHierarchy goodsHierarchy;

    private Duration duration;

}
