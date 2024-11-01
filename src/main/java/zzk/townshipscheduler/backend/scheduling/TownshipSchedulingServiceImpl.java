package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.backend.persistence.Order;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.backend.service.GoodsService;
import zzk.townshipscheduler.backend.tfdemo.foodpacking.PackagingSchedule;
import zzk.townshipscheduler.port.GoodId;
import zzk.townshipscheduler.port.GoodsHierarchy;
import zzk.townshipscheduler.port.form.BillScheduleRequest;

import java.util.*;
import java.util.stream.Collectors;

@Service(value = "schedulingService")
@RequiredArgsConstructor
public class TownshipSchedulingServiceImpl implements ITownshipSchedulingService {

    private final GoodsService goodsService;

    private final SolverManager<TownshipSchedulingProblem, UUID> solverManager;

    private final SolutionManager<TownshipSchedulingProblem, HardMediumSoftLongScore> solutionManager;

    @Override
    public UUID prepareScheduling(BillScheduleRequest billScheduleRequest) {
        TownshipSchedulingProblem townshipSchedulingProblem = new TownshipSchedulingProblem();

        List<Goods> goodsList = goodsService.findBy(Goods.class);

        Map<Goods, SchedulingGoods> tempContrastMap = new HashMap<>();
        List<SchedulingGoods> schedulingGoodsList = new ArrayList<>();

        for (Goods goods : goodsList) {
            SchedulingGoods schedulingGoods = new SchedulingGoods(goods);
            schedulingGoods.setProducingDuration(goodsService.calcGoodsDuration(goods));
            GoodsHierarchy goodsHierarchy = goodsService.calcGoodsHierarchies(goods);
            schedulingGoods.setGoodsHierarchy(goodsHierarchy);
            schedulingGoodsList.add(schedulingGoods);
            tempContrastMap.put(goods, schedulingGoods);
        }

        for (SchedulingGoods schedulingGoods : schedulingGoodsList) {
            Map<SchedulingGoods, Integer> bom = new LinkedHashMap<>();
            schedulingGoods.getGoodsHierarchy()
                    .getMaterials()
                    .forEach((goodId, integer) -> {
                        bom.put(
                                schedulingGoodsList.stream().filter(goods -> goodId.equals(goods.getGoodId())).findFirst().orElseThrow(),
                                integer
                        );
                    });
            schedulingGoods.setBom(bom);
        }
        townshipSchedulingProblem.setGoods(schedulingGoodsList);

        Map<String, List<Goods>> categoryGoodsListMap = goodsList.stream().collect(Collectors.groupingBy(Goods::getCategory));
        List<SchedulingPlantFieldSlot> plantFieldSlots = new ArrayList<>();
        categoryGoodsListMap.forEach((category, portfolios) -> {
            for (int i = 0; i < 6; i++) {
                SchedulingPlantFieldSlot slot = new SchedulingPlantFieldSlot();
                slot.setCategory(category);
                slot.setCategorySeq(i + 1);
                slot.setPortfolioGoods(portfolios.stream().map(tempContrastMap::get).toList());
                slot.setParallel(1);
                plantFieldSlots.add(slot);
            }
        });
        townshipSchedulingProblem.setPlantSlots(plantFieldSlots);

        List<Order> orders = billScheduleRequest.getOrders();
        List<SchedulingOrder> schedulingOrders = orders.stream()
                .map(order -> {
                    Map<Goods, Integer> productAmountPairs = order.getProductAmountPairs();
                    Map<SchedulingGoods, Integer> map = new LinkedHashMap<>();
                    productAmountPairs.forEach((goods, integer) -> {
                        map.put(tempContrastMap.get(goods), integer);
                    });
                    return new SchedulingOrder(
                            order.getId(),
                            order.getOrderType().name(),
                            map,
                            order.getDeadLine()
                    );
                }).toList();
        townshipSchedulingProblem.setOrders(schedulingOrders);

        List<SchedulingProducing> schedulingProducingList = schedulingGoodsList.stream()
                .map(SchedulingProducing::new)
                .toList();
        townshipSchedulingProblem.setSchedulingProducingList(schedulingProducingList);

        UUID uuid = UUID.randomUUID();
        townshipSchedulingProblem.setUuid(uuid);
        return uuid;
    }

    @Override
    public boolean checkUuidIsValidForSchedule(String uuid) {
        return false;
    }


}
