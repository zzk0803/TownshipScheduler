package zzk.townshipscheduler.backend.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
class ProblemTransferProcess {

    public static final int MINUTE_GRAIN = 5;

    private final TownshipSchedulingRequest townshipSchedulingRequest;

    private final Map<SchedulingProduct.Id, SchedulingProduct> idProductMap;

    private final Map<SchedulingFactoryInfo.Id, SchedulingFactoryInfo> idFactoryTypeMap;

    private final List<SchedulingProduct> schedulingProductList;

    private final List<SchedulingFactoryInfo> schedulingFactoryInfoList;

    private final List<SchedulingProducingExecutionMode> schedulingProducingExecutionModes;

    private final List<SchedulingFactoryInstance> schedulingFactoryInstances;

    private final List<SchedulingOrder> schedulingOrders;

    private final DateTimeSlotSize dateTimeSlotSize;

    private final LocalDateTime workCalendarStart;

    private final LocalDateTime workCalendarEnd;

    private final LocalTime sleepStartPickerValue;

    private final LocalTime sleepEndPickerValue;

    private SchedulingPlayer schedulingPlayer;

    public ProblemTransferProcess(
            TownshipSchedulingRequest townshipSchedulingRequest
    ) {
        this.townshipSchedulingRequest = townshipSchedulingRequest;
        this.dateTimeSlotSize = townshipSchedulingRequest.getDateTimeSlotSize();
        this.workCalendarStart = townshipSchedulingRequest.getWorkCalendarStart();
        this.workCalendarEnd = townshipSchedulingRequest.getWorkCalendarEnd();
        this.sleepStartPickerValue = townshipSchedulingRequest.getSleepStartPickerValue();
        this.sleepEndPickerValue = townshipSchedulingRequest.getSleepEndPickerValue();
        this.idProductMap = new HashMap<>();
        this.idFactoryTypeMap = new HashMap<>();
        this.schedulingProducingExecutionModes = new ArrayList<>();
        this.schedulingFactoryInstances = new ArrayList<>();
        this.schedulingOrders = new ArrayList<>();
        this.schedulingProductList = new ArrayList<>();
        this.schedulingFactoryInfoList = new ArrayList<>();
    }

    public TownshipSchedulingProblem buildProblem() {
        doMapping();
        this.schedulingProductList.addAll(
                new ArrayList<>(this.idProductMap.values())
        );
        this.schedulingFactoryInfoList.addAll(
                new ArrayList<>(this.idFactoryTypeMap.values())
        );

        SchedulingWorkCalendar schedulingWorkCalendar
                = SchedulingWorkCalendar.with(workCalendarStart, workCalendarEnd);

        this.schedulingPlayer.setSleepStart(this.sleepStartPickerValue);
        this.schedulingPlayer.setSleepEnd(this.sleepEndPickerValue);

        return TownshipSchedulingProblem.builder()
                .uuid()
                .schedulingProductList(new ArrayList<>(this.schedulingProductList))
                .schedulingFactoryInfoList(new ArrayList<>(this.schedulingFactoryInfoList))
                .schedulingOrderList(new ArrayList<>(this.schedulingOrders))
                .schedulingFactoryInstanceList(new ArrayList<>(this.schedulingFactoryInstances))
                .schedulingPlayer(this.schedulingPlayer)
                .schedulingWorkCalendar(schedulingWorkCalendar)
                .dateTimeSlotSize(this.dateTimeSlotSize)
                .build();
    }

    private void doMapping() {
        fetchAndMapToSchedulingProduct();
        fetchAndMapToSchedulingFactoryInfo();
        fetchAndMapToSchedulingFactoryInstance();
        fetchAndMapToSchedulingWarehouse();
        fetchAndMapToSchedulingOrder();
    }

    private void fetchAndMapToSchedulingProduct() {
        Collection<ProductEntity> productDtoList = townshipSchedulingRequest.getProductEntities();
        for (ProductEntity productDto : productDtoList) {
            SchedulingProduct schedulingProduct = buildOrGetSchedulingProduct(productDto);

            FieldFactoryInfoEntity fieldFactoryInfo = productDto.getFieldFactoryInfo();
            schedulingProduct.setRequireFactory(buildOrGetSchedulingFactoryInfo(fieldFactoryInfo));

            Set<SchedulingProducingExecutionMode> producingExecutionModes
                    = calcProducingExecutionMode(productDto, schedulingProduct);
            schedulingProduct.setExecutionModeSet(producingExecutionModes);
            this.schedulingProducingExecutionModes.addAll(producingExecutionModes);
        }
    }

    private SchedulingProduct buildOrGetSchedulingProduct(ProductEntity product) {
        return idProductMap.computeIfAbsent(
                SchedulingProduct.Id.of(product),
                id -> {
                    SchedulingProduct schedulingProduct = new SchedulingProduct();
                    schedulingProduct.setId(id);
                    schedulingProduct.setName(product.getName());
                    schedulingProduct.setLevel(product.getLevel());
                    schedulingProduct.setGainWhenCompleted(product.getDefaultAmountWhenCreated());
                    return schedulingProduct;
                }
        );

    }

    private Set<SchedulingProducingExecutionMode> calcProducingExecutionMode(
            ProductEntity productEntity,
            SchedulingProduct schedulingProduct
    ) {
        AtomicInteger idRoller = new AtomicInteger(1);
        Set<ProductManufactureInfoEntity> productManufactureInfos = productEntity.getManufactureInfoEntities();
        if (productManufactureInfos != null) {
            Set<SchedulingProducingExecutionMode> executionModes = new LinkedHashSet<>();
            productManufactureInfos.forEach(productManufactureInfo -> {
                SchedulingProducingExecutionMode executionMode = new SchedulingProducingExecutionMode();
                executionMode.setId(idRoller.getAndIncrement());
                executionMode.setProduct(schedulingProduct);
                Duration producingDuration = productManufactureInfo.getProducingDuration();
                executionMode.setExecuteDuration(producingDuration != null ? producingDuration : Duration.ZERO);
                ProductAmountBill productAmountBill = new ProductAmountBill();
                executionMode.setMaterials(productAmountBill);
                Set<ProductMaterialsRelation> productMaterialsRelations = productManufactureInfo.getProductMaterialsRelations();
                if (productMaterialsRelations != null && !productMaterialsRelations.isEmpty()) {
                    productMaterialsRelations.forEach(productMaterialsRelation -> {
                        ProductEntity materialProduct = productMaterialsRelation.getMaterial();
                        Integer amount = productMaterialsRelation.getAmount();
                        SchedulingProduct material = buildOrGetSchedulingProduct(materialProduct);
                        productAmountBill.put(material, amount);
                    });
                }
                executionModes.add(executionMode);
            });
            return executionModes;
        } else {
            SchedulingProducingExecutionMode defaultProducingExecutionMode = new SchedulingProducingExecutionMode();
            defaultProducingExecutionMode.setId(idRoller.getAndIncrement());
            defaultProducingExecutionMode.setProduct(schedulingProduct);
            return Set.of(defaultProducingExecutionMode);
        }

    }

    private void fetchAndMapToSchedulingFactoryInfo() {
        Collection<FieldFactoryInfoEntity> factoryInfoEntities
                = townshipSchedulingRequest.getFieldFactoryInfoEntities();
        factoryInfoEntities.forEach(
                fieldFactoryInfo -> {
                    SchedulingFactoryInfo schedulingFactoryInfo = buildOrGetSchedulingFactoryInfo(fieldFactoryInfo);
                    fieldFactoryInfo.getPortfolioGoods().stream()
                            .map(this::buildOrGetSchedulingProduct)
                            .forEach(schedulingFactoryInfo::appendPortfolioProduct);
                    schedulingFactoryInfo.setProducingStructureType(fieldFactoryInfo.getProducingType());
                    schedulingFactoryInfo.setDefaultInstanceAmount(fieldFactoryInfo.getDefaultInstanceAmount());
                    schedulingFactoryInfo.setDefaultProducingCapacity(fieldFactoryInfo.getDefaultProducingCapacity());
                    schedulingFactoryInfo.setDefaultReapWindowCapacity(fieldFactoryInfo.getDefaultReapWindowCapacity());
                    schedulingFactoryInfo.setMaxInstanceAmount(fieldFactoryInfo.getMaxInstanceAmount());
                    schedulingFactoryInfo.setMaxProducingCapacity(fieldFactoryInfo.getMaxProducingCapacity());
                    schedulingFactoryInfo.setMaxReapWindowCapacity(fieldFactoryInfo.getMaxReapWindowCapacity());
                }
        );
    }

    private SchedulingFactoryInfo buildOrGetSchedulingFactoryInfo(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
        return idFactoryTypeMap.computeIfAbsent(
                SchedulingFactoryInfo.Id.of(fieldFactoryInfoEntity),
                id -> {
                    SchedulingFactoryInfo info = new SchedulingFactoryInfo();
                    info.setId(id);
                    info.setCategoryName(fieldFactoryInfoEntity.getCategory());
                    info.setLevel(fieldFactoryInfoEntity.getLevel());
                    return info;
                }
        );
    }

    private void fetchAndMapToSchedulingFactoryInstance() {
        AtomicInteger factoryInstanceIdRoller = new AtomicInteger(1);
        townshipSchedulingRequest.getPlayerEntityFieldFactoryEntities()
                .stream()
                .collect(Collectors.partitioningBy(
                        fieldFactoryEntity -> {
                            FieldFactoryInfoEntity factoryInfoEntity = fieldFactoryEntity.getFieldFactoryInfoEntity();
                            String category = factoryInfoEntity.getCategory();
                            return FieldFactoryInfoEntity.FIELD_CATEGORY_CRITERIA.equalsIgnoreCase(category);
                        })
                )
                .forEach(
                        (boolFieldType, fieldFactoryEntities) -> {
                            if (boolFieldType) {
                                Assert.isTrue(
                                        fieldFactoryEntities.stream()
                                                .allMatch(
                                                        fieldFactoryEntity -> fieldFactoryEntity.getFieldFactoryInfoEntity()
                                                                .getCategory()
                                                                .equalsIgnoreCase(FieldFactoryInfoEntity.FIELD_CATEGORY_CRITERIA)
                                                ),
                                        "All Entity Should Be Field Type!?"
                                );
                                int size = fieldFactoryEntities.size();
                                FieldFactoryEntity fieldFactoryEntity = fieldFactoryEntities.stream()
                                        .findFirst()
                                        .orElseThrow();
                                SchedulingFactoryInstance fieldInstance = new SchedulingFactoryInstance();
                                SchedulingFactoryInfo schedulingFactoryInfo
                                        = buildOrGetSchedulingFactoryInfo(fieldFactoryEntity.getFieldFactoryInfoEntity());
                                int factoryInstanceId = factoryInstanceIdRoller.getAndIncrement();
                                fieldInstance.setId(factoryInstanceId);
                                fieldInstance.setSchedulingFactoryInfo(schedulingFactoryInfo);
                                fieldInstance.setProducingQueue(size);
                                fieldInstance.setReapWindowSize(size);
                                fieldInstance.setSeqNum(1);
                                fieldInstance.setupFactoryReadableIdentifier();
                                schedulingFactoryInfo.getFactoryInstances().add(fieldInstance);
                                this.schedulingFactoryInstances.add(fieldInstance);
                            } else {
                                Map<FieldFactoryInfoEntity, List<FieldFactoryEntity>> typeInstanceMap
                                        = fieldFactoryEntities.stream()
                                        .collect(Collectors.groupingBy(FieldFactoryEntity::getFieldFactoryInfoEntity));
                                for (Map.Entry<FieldFactoryInfoEntity, List<FieldFactoryEntity>> entry : typeInstanceMap.entrySet()) {
                                    FieldFactoryInfoEntity type = entry.getKey();
                                    List<FieldFactoryEntity> instanceList = entry.getValue();
                                    int size = instanceList.size();
                                    SchedulingFactoryInfo schedulingFactoryInfo
                                            = buildOrGetSchedulingFactoryInfo(type);
                                    for (int i = 0; i < size; i++) {
                                        FieldFactoryEntity fieldFactoryEntity = instanceList.get(i);
                                        SchedulingFactoryInstance factoryInstance
                                                = new SchedulingFactoryInstance();
                                        factoryInstance.setId(factoryInstanceIdRoller.getAndIncrement());
                                        factoryInstance.setSeqNum(i + 1);
                                        factoryInstance.setSchedulingFactoryInfo(
                                                schedulingFactoryInfo
                                        );
                                        factoryInstance.setProducingQueue(
                                                fieldFactoryEntity.getProducingLength()
                                        );
                                        factoryInstance.setReapWindowSize(
                                                fieldFactoryEntity.getReapWindowSize()
                                        );
                                        schedulingFactoryInfo.getFactoryInstances()
                                                .add(factoryInstance);
                                        factoryInstance.setupFactoryReadableIdentifier();
                                        this.schedulingFactoryInstances.add(factoryInstance);
                                    }
                                }
                            }
                        }
                );

    }

    private void fetchAndMapToSchedulingWarehouse() {
        SchedulingPlayer schedulingPlayer = new SchedulingPlayer();
        Map<SchedulingProduct, Integer> productAmountMap = new LinkedHashMap<>();

        WarehouseEntity warehouseEntityProjection
                = townshipSchedulingRequest.getPlayerEntityWarehouseEntity();
        Map<ProductEntity, Integer> dtoMap = warehouseEntityProjection.getProductAmountMap();
        dtoMap.forEach(
                (product, amount) -> {
                    productAmountMap.put(
                            buildOrGetSchedulingProduct(product),
                            amount
                    );
                }
        );
        this.schedulingPlayer = schedulingPlayer;
    }

    private void fetchAndMapToSchedulingOrder() {
        townshipSchedulingRequest.getPlayerEntityOrderEntities()
                .forEach(orderEntity -> {
                    Long id = orderEntity.getId();
                    OrderType orderType = orderEntity.getOrderType();
                    LocalDateTime deadLine = orderEntity.getDeadLine();
                    ProductAmountBill productAmountBill = createProductAmountBill(orderEntity);
                    this.schedulingOrders.add(
                            new SchedulingOrder(
                                    id,
                                    productAmountBill,
                                    orderType,
                                    deadLine
                            ));
                });
    }

    private ProductAmountBill createProductAmountBill(OrderEntity order) {
        Map<ProductEntity, Integer> productIdAmountMap = order.getProductAmountMap();
        ProductAmountBill productAmountBill = new ProductAmountBill();
        productIdAmountMap.forEach(
                (product, amount) -> {
                    productAmountBill.put(
                            buildOrGetSchedulingProduct(product),
                            amount
                    );
                }
        );
        return productAmountBill;
    }

    public LocalDateTime calcCalendarStart() {
        LocalDateTime localDateTimeNow = LocalDateTime.now();
        LocalDateTime after30Mins = localDateTimeNow.plusMinutes(30);
        int i1 = after30Mins.getMinute() % MINUTE_GRAIN;
        int i2 = MINUTE_GRAIN - i1;
        return after30Mins.plusMinutes(i2);
    }

}
