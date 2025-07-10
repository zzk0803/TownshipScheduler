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
        this.idProductMap = new LinkedHashMap<>();
        this.idFactoryTypeMap = new LinkedHashMap<>();
        this.schedulingProducingExecutionModes = new ArrayList<>();
        this.schedulingFactoryInstances = new ArrayList<>();
        this.schedulingOrders = new ArrayList<>();
    }

    public TownshipSchedulingProblem buildProblem() {
        doMapping();

        SchedulingWorkCalendar schedulingWorkCalendar
                = SchedulingWorkCalendar.with(workCalendarStart, workCalendarEnd);

        this.schedulingPlayer.setSleepStart(this.sleepStartPickerValue);
        this.schedulingPlayer.setSleepEnd(this.sleepEndPickerValue);

        return TownshipSchedulingProblem.builder()
                .uuid()
                .schedulingProductList(new ArrayList<>(this.idProductMap.values()))
                .schedulingFactoryInfoList(new ArrayList<>(this.idFactoryTypeMap.values()))
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
        SchedulingProduct schedulingProduct = null;
        for (ProductEntity productDto : productDtoList) {
            schedulingProduct = buildOrGetSchedulingProduct(productDto);

            FieldFactoryInfoEntity fieldFactoryInfo
                    = productDto.getFieldFactoryInfo();
            SchedulingFactoryInfo schedulingFactoryInfo
                    = buildOrGetSchedulingFactoryInfo(
                    SchedulingFactoryInfo.Id.of(fieldFactoryInfo)
            );
            schedulingProduct.setRequireFactory(schedulingFactoryInfo);

            Set<SchedulingProducingExecutionMode> producingExecutionModes
                    = calcProducingExecutionMode(productDto, schedulingProduct);
            schedulingProduct.setExecutionModeSet(producingExecutionModes);
            this.schedulingProducingExecutionModes.addAll(producingExecutionModes);
        }
    }

    private void fetchAndMapToSchedulingFactoryInfo() {
        Collection<FieldFactoryInfoEntity> factoryInfoEntities
                = townshipSchedulingRequest.getFieldFactoryInfoEntities();
//            Set<FieldFactoryInfoEntityDto> factoryInfoEntities = townshipSchedulingRequest.getFieldFactoryInfoEntities();
        for (FieldFactoryInfoEntity fieldFactoryInfo : factoryInfoEntities) {
//            for (FieldFactoryInfoEntityDto fieldFactoryInfo : factoryInfoEntities) {
            SchedulingFactoryInfo schedulingFactoryInfo = buildOrGetSchedulingFactoryInfo(fieldFactoryInfo);
            fieldFactoryInfo.getPortfolioGoods().stream()
                    .map(product -> this.buildOrGetSchedulingProduct(
                            SchedulingProduct.Id.of(product.getId())
                    ))
                    .forEach(schedulingFactoryInfo::appendPortfolioProduct);
            schedulingFactoryInfo.setProducingStructureType(fieldFactoryInfo.getProducingType());
            schedulingFactoryInfo.setDefaultInstanceAmount(fieldFactoryInfo.getDefaultInstanceAmount());
            schedulingFactoryInfo.setDefaultProducingCapacity(fieldFactoryInfo.getDefaultProducingCapacity());
            schedulingFactoryInfo.setDefaultReapWindowCapacity(fieldFactoryInfo.getDefaultReapWindowCapacity());
            schedulingFactoryInfo.setMaxInstanceAmount(fieldFactoryInfo.getMaxInstanceAmount());
            schedulingFactoryInfo.setMaxProducingCapacity(fieldFactoryInfo.getMaxProducingCapacity());
            schedulingFactoryInfo.setMaxReapWindowCapacity(fieldFactoryInfo.getMaxReapWindowCapacity());
        }
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
                                SchedulingFactoryInfo.Id schedulingFactoryInfoId
                                        = SchedulingFactoryInfo.Id.of(fieldFactoryEntity.getFieldFactoryInfoEntity());
                                SchedulingFactoryInfo schedulingFactoryInfo
                                        = buildOrGetSchedulingFactoryInfo(schedulingFactoryInfoId);
                                int factoryInstanceId = factoryInstanceIdRoller.getAndIncrement();
                                fieldInstance.setId(factoryInstanceId);
                                fieldInstance.setSchedulingFactoryInfo(schedulingFactoryInfo);
                                fieldInstance.setProducingLength(size);
                                fieldInstance.setReapWindowSize(size);
                                fieldInstance.setSeqNum(1);
                                fieldInstance.setupFactoryReadableIdentifier();
                                schedulingFactoryInfo.getFactoryInstances().add(fieldInstance);
                                this.schedulingFactoryInstances.add(fieldInstance);
                            } else {
                                Map<FieldFactoryInfoEntity, List<FieldFactoryEntity>> typeInstamceMap
                                        = fieldFactoryEntities
                                        .stream()
                                        .collect(Collectors.groupingBy(FieldFactoryEntity::getFieldFactoryInfoEntity));
                                for (Map.Entry<FieldFactoryInfoEntity, List<FieldFactoryEntity>> entry : typeInstamceMap.entrySet()) {
                                    FieldFactoryInfoEntity type = entry.getKey();
                                    List<FieldFactoryEntity> instanceList = entry.getValue();
                                    int size = instanceList.size();
                                    SchedulingFactoryInfo.Id schedulingFactoryInfoId
                                            = SchedulingFactoryInfo.Id.of(type);
                                    SchedulingFactoryInfo schedulingFactoryInfo
                                            = buildOrGetSchedulingFactoryInfo(schedulingFactoryInfoId);
                                    String categoryName = schedulingFactoryInfo.getCategoryName();
                                    for (int i = 0; i < size; i++) {
                                        FieldFactoryEntity fieldFactoryEntity = instanceList.get(i);
                                        SchedulingFactoryInstance factoryInstance
                                                = new SchedulingFactoryInstance();
                                        factoryInstance.setId(factoryInstanceIdRoller.getAndIncrement());
                                        int seqNum = i + 1;
                                        factoryInstance.setSeqNum(seqNum);
                                        factoryInstance.setSchedulingFactoryInfo(
                                                schedulingFactoryInfo
                                        );
                                        factoryInstance.setProducingLength(
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
                            buildOrGetSchedulingProduct(SchedulingProduct.Id.of(product.getId())),
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

    private SchedulingProduct buildOrGetSchedulingProduct(ProductEntity product) {
        SchedulingProduct.Id productId = SchedulingProduct.Id.of(Objects.requireNonNull(product.getId()));
        SchedulingProduct schedulingProduct = buildOrGetSchedulingProduct(productId);
        schedulingProduct.setName(product.getName());
        schedulingProduct.setLevel(product.getLevel());
        return schedulingProduct;

    }

    private SchedulingFactoryInfo buildOrGetSchedulingFactoryInfo(SchedulingFactoryInfo.Id schedulingFactoryInfoId) {
        if (!idFactoryTypeMap.containsKey(schedulingFactoryInfoId)) {
            SchedulingFactoryInfo schedulingFactoryInfo = new SchedulingFactoryInfo();
            schedulingFactoryInfo.setId(schedulingFactoryInfoId);
            idFactoryTypeMap.put(schedulingFactoryInfoId, schedulingFactoryInfo);
        }
        return idFactoryTypeMap.get(schedulingFactoryInfoId);

    }

    private Set<SchedulingProducingExecutionMode> calcProducingExecutionMode(
            ProductEntity productEntity,
            SchedulingProduct schedulingProduct
    ) {
        AtomicInteger idRoller = new AtomicInteger(1);
        Set<ProductManufactureInfoEntity> productManufactureInfos = productEntity.getManufactureInfoEntities();
        if (productManufactureInfos != null && !productManufactureInfos.isEmpty()) {
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
                        SchedulingProduct material
                                = buildOrGetSchedulingProduct(
                                SchedulingProduct.Id.of(materialProduct)
                        );
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

    private SchedulingFactoryInfo buildOrGetSchedulingFactoryInfo(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
        SchedulingFactoryInfo schedulingFactoryInfo
                = buildOrGetSchedulingFactoryInfo(SchedulingFactoryInfo.Id.of(fieldFactoryInfoEntity));
        schedulingFactoryInfo.setCategoryName(fieldFactoryInfoEntity.getCategory());
        schedulingFactoryInfo.setLevel(fieldFactoryInfoEntity.getLevel());
        return schedulingFactoryInfo;
    }

    private SchedulingProduct buildOrGetSchedulingProduct(SchedulingProduct.Id schedulingProductId) {
        if (schedulingProductId == null) {
            throw new IllegalArgumentException();
        }

        if (!idProductMap.containsKey(schedulingProductId)) {
            SchedulingProduct schedulingProduct = new SchedulingProduct();
            schedulingProduct.setId(schedulingProductId);
            idProductMap.put(schedulingProductId, schedulingProduct);
        }
        return idProductMap.get(schedulingProductId);
    }

    private ProductAmountBill createProductAmountBill(OrderEntity order) {
        Map<ProductEntity, Integer> productIdAmountMap = order.getProductAmountMap();
        ProductAmountBill productAmountBill = new ProductAmountBill();
        productIdAmountMap.forEach(
                (product, amount) -> {
                    productAmountBill.put(
                            buildOrGetSchedulingProduct(SchedulingProduct.Id.of(product.getId())),
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
