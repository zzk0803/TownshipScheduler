package zzk.townshipscheduler.backend.scheduling.mapping;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.backend.persistence.GoodsHierarchy;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingGoods;
import zzk.townshipscheduler.backend.service.GoodsService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class GoodsMapperAroundProcess {

    private final GoodsMapper goodsMapper;

    private final GoodsService goodsService;

    private SchedulingGoods advanceProcess(Goods goods) {
        SchedulingGoods sg = goodsMapper.toDto(goods);
        Duration producingDuration = goodsService.queryDuration(goods);
        GoodsHierarchy goodsHierarchy = goodsService.calcGoodsHierarchies(goods);
        sg.setGoodsHierarchy(goodsHierarchy);
        sg.setDuration(producingDuration);
        return sg;
    }

}
