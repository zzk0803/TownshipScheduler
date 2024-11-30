package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.backend.service.ProductService;
import zzk.townshipscheduler.pojo.form.BillScheduleRequest;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TownshipSchedulingServiceImpl implements ITownshipSchedulingService {

    private final ProductService productService;

    private final TownshipSchedulingProblemHolder problemHolder;

    private final SolverManager<TownshipSchedulingProblem, UUID> solverManager;

    private final SolutionManager<TownshipSchedulingProblem, HardMediumSoftLongScore> solutionManager;

    @Override
    public UUID prepareScheduling(BillScheduleRequest billScheduleRequest) {
//        TownshipSchedulingProblem townshipSchedulingProblem = new TownshipSchedulingProblem();
//        SchedulingGamePlayer schedulingGamePlayer = new SchedulingGamePlayer();
//        schedulingGamePlayer.setPlayerName("TEST");
//        schedulingGamePlayer.setPlayerLevel(72);
//        townshipSchedulingProblem.setSchedulingGamePlayer(schedulingGamePlayer);
//
////        List<ProductEntity> productEntityList = goodsService.findBy(ProductEntity.class);
//        List<ProductEntityForSchedulingDto> productEntityDtoList = goodsService.findBy(ProductEntityForSchedulingDto.class);
//
//        Map<ProductEntity, SchedulingProduct> tempContrastMapEntitySchedulingMap = new HashMap<>();
//        Map<ProductId, SchedulingProduct> tempContrastMapGoodsIdSchedulingMap = new HashMap<>();
//        List<SchedulingProduct> schedulingProductList = new ArrayList<>();
//
////        for (ProductEntity productEntity : productEntityList) {
////            SchedulingProduct schedulingProduct = new SchedulingProduct(productEntity);
////            schedulingProduct.setProducingDuration(goodsService.calcGoodsDuration(productEntity).get(0));
////            schedulingProduct.setProductManufactureRelation(goodsService.calcGoodsHierarchies(productEntity).get(0));
////            schedulingProductList.add(schedulingProduct);
////            tempContrastMapEntitySchedulingMap.put(productEntity, schedulingProduct);
////            tempContrastMapGoodsIdSchedulingMap.put(ProductId.of(schedulingProduct.getId()), schedulingProduct);
////        }
//        for (ProductEntityForSchedulingDto productEntityDto : productEntityDtoList) {
//            SchedulingProduct schedulingProduct = new SchedulingProduct(productEntityDto);
//            schedulingProduct.setProducingDuration(goodsService.calcGoodsDuration(productEntity).get(0));
//            schedulingProduct.setProductManufactureRelation(goodsService.calcGoodsHierarchies(productEntity).get(0));
//            schedulingProductList.add(schedulingProduct);
//            tempContrastMapEntitySchedulingMap.put(productEntity, schedulingProduct);
//            tempContrastMapGoodsIdSchedulingMap.put(ProductId.of(schedulingProduct.getId()), schedulingProduct);
//        }
//
//        for (SchedulingProduct schedulingProduct : schedulingProductList) {
//            Map<SchedulingProduct, Integer> bom = new LinkedHashMap<>();
//            ProductManufactureRelation productManufactureRelation = schedulingProduct.getProductManufactureRelation();
//            if (Objects.nonNull(productManufactureRelation)) {
//                Map<ProductId, Integer> mapInHierarchy = productManufactureRelation.getMaterials();
//                if (Objects.nonNull(mapInHierarchy) && !mapInHierarchy.isEmpty()) {
//                    mapInHierarchy.forEach((goodId, amount) -> bom.put(
//                            tempContrastMapGoodsIdSchedulingMap.get(goodId),
//                            amount
//                    ));
//                    schedulingProduct.setMaterialAmountMap(bom);
//                }
//            }
//        }
//        townshipSchedulingProblem.setSchedulingProductList(schedulingProductList);
//
//        List<SchedulingFactory> schedulingFactoryList = new ArrayList<>();
//        Map<String, List<SchedulingProduct>> categorySchedulingProductsMap = productEntityList.stream()
//                .map(tempContrastMapEntitySchedulingMap::get)
//                .collect(Collectors.groupingBy(SchedulingProduct::getCategory));
//        categorySchedulingProductsMap.forEach(
//                (category, portfolios) -> {
//                    int slotAmount = 6;
//                    SchedulingFactory schedulingFactory = new SchedulingFactory(
//                            category,
//                            portfolios,
//                            slotAmount
//                    );
//                    List<SchedulingFactorySlot> schedulingFactorySlotList = new ArrayList<>();
//                    for (int i = 0; i < slotAmount; i++) {
//                        SchedulingFactorySlot schedulingFactorySlot = new SchedulingFactorySlot();
//                        schedulingFactorySlot.setId(schedulingFactory.getCategory() + "-" + (1 + i));
//                        schedulingFactorySlot.setSchedulingFactory(schedulingFactory);
//                        schedulingFactorySlot.setPlayer(schedulingGamePlayer);
//                        schedulingFactorySlotList.add(schedulingFactorySlot);
//                    }
//                    schedulingFactory.setFactorySlotList(schedulingFactorySlotList);
//                    schedulingFactoryList.add(schedulingFactory);
//                }
//        );
//        townshipSchedulingProblem.setSchedulingFactoryList(schedulingFactoryList);
//
//        List<OrderEntity> orderEntities = billScheduleRequest.getOrderEntities();
//        List<SchedulingOrder> schedulingOrders = orderEntities.stream()
//                .map(order -> {
//                    Map<ProductEntity, Integer> productAmountPairs = order.getProductAmountPairs();
//                    Map<SchedulingProduct, Integer> billInOrder = new LinkedHashMap<>();
//                    productAmountPairs.forEach((goods, amount) -> billInOrder.put(tempContrastMapEntitySchedulingMap.get(
//                            goods), amount));
//                    return new SchedulingOrder(
//                            order.getId(),
//                            order.getOrderType().name(),
//                            billInOrder,
//                            order.getDeadLine()
//                    );
//                }).toList();
//        townshipSchedulingProblem.setSchedulingOrderList(schedulingOrders);
//
//        List<SchedulingFactorySlot> schedulingFactorySlotList = schedulingFactoryList.stream()
//                .flatMap(schedulingFactory -> schedulingFactory.getFactorySlotList().stream())
//                .toList();
//        townshipSchedulingProblem.setSchedulingFactorySlotList(schedulingFactorySlotList);
//
//        List<SchedulingProducing> schedulingProducingList = schedulingOrders.stream()
//                .flatMap(schedulingOrder -> schedulingOrder.calcProducingGoods().stream())
//                .toList();
//        schedulingGamePlayer.setSchedulingProducingList(schedulingProducingList);
//        townshipSchedulingProblem.setSchedulingProducingList(schedulingProducingList);
//        log.info("{} producing,details:{}", schedulingProducingList.size(), schedulingProducingList);
//
//
//        UUID uuid = UUID.randomUUID();
//        townshipSchedulingProblem.setUuid(uuid);
//        problemHolder.write(townshipSchedulingProblem);
//        return uuid;
        return UUID.randomUUID();
    }

    @Override
    public CompletableFuture<Void> scheduling(UUID problemId) {
        return CompletableFuture.runAsync(() -> {
            solverManager.solveBuilder()
                    .withProblemId(problemId)
                    .withProblemFinder(this::getSchedule)
                    .withBestSolutionConsumer(problemHolder::write)
                    .run();
        });
    }

    @Override
    public CompletableFuture<Void> abort(UUID problemId) {
        return CompletableFuture.runAsync(() -> {
            solverManager.terminateEarly(problemId);
        });
    }

    @Override
    public TownshipSchedulingProblem getSchedule(UUID problemId) {
        SolverStatus solverStatus = solverManager.getSolverStatus(problemId);
        TownshipSchedulingProblem townshipSchedulingProblem = problemHolder.read();
        townshipSchedulingProblem.setSolverStatus(solverStatus);
        return townshipSchedulingProblem;
    }

    @Override
    public boolean checkUuidIsValidForSchedule(String uuid) {
        if (Objects.isNull(uuid) || uuid.isBlank()) {
            return false;
        }

        TownshipSchedulingProblem problem = problemHolder.read();
        if (Objects.isNull(problem)) {
            return false;
        }
        return problem.getUuid().toString().equals(uuid);
    }


}
