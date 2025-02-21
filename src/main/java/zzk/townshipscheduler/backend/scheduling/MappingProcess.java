package zzk.townshipscheduler.backend.scheduling;

import org.springframework.util.Assert;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.persistence.select.*;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class MappingProcess {

    private final TownshipSchedulingRequest townshipSchedulingRequest;

    private final Map<SchedulingProduct.Id, SchedulingProduct> idProductMap;

    private final Map<SchedulingFactoryInfo.Id, SchedulingFactoryInfo> idFactoryTypeMap;

    private final Set<SchedulingProducingExecutionMode> schedulingProducingExecutionModes;

    private final Set<SchedulingFactoryInstance> schedulingFactoryInstances;

    private final Set<SchedulingOrder> schedulingOrders;

    private SchedulingWarehouse schedulingWarehouse;

    public MappingProcess(TownshipSchedulingRequest townshipSchedulingRequest) {
        this.townshipSchedulingRequest = townshipSchedulingRequest;
        this.idProductMap = new LinkedHashMap<>();
        this.idFactoryTypeMap = new LinkedHashMap<>();
        this.schedulingProducingExecutionModes = new LinkedHashSet<>();
        this.schedulingFactoryInstances = new LinkedHashSet<>();
        this.schedulingOrders = new LinkedHashSet<>();
    }

    public TownshipSchedulingProblem map() {
        doMapping();
        TownshipSchedulingProblem schedulingProblem
                = new TownshipSchedulingProblem(
                new LinkedHashSet<>(this.idProductMap.values()),
                new LinkedHashSet<>(this.idFactoryTypeMap.values()),
                this.schedulingOrders,
                this.schedulingFactoryInstances,
                this.schedulingWarehouse,
                new SchedulingWorkTimeLimit(
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(3)
                ),
                TownshipSchedulingProblem.DateTimeSlotSize.BIG
        );

        return schedulingProblem;
    }


    private void doMapping() {
        fetchAndMapToSchedulingProduct();
        fetchAndMapToSchedulingFactoryInfo();
        fetchAndMapToSchedulingFactoryInstance();
        fetchAndMapToSchedulingWarehouse();
        fetchAndMapToSchedulingOrder();
    }

    private void fetchAndMapToSchedulingProduct() {
        Set<ProductEntity> productDtoList = townshipSchedulingRequest.getProductEntities();
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
        Set<FieldFactoryInfoEntity> factoryInfoEntities
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
                                SchedulingFactoryInstance schedulingFactoryInstance = new SchedulingFactoryInstance();
                                SchedulingFactoryInfo.Id schedulingFactoryInfoId
                                        = SchedulingFactoryInfo.Id.of(fieldFactoryEntity.getFieldFactoryInfoEntity());
                                SchedulingFactoryInfo schedulingFactoryInfo
                                        = buildOrGetSchedulingFactoryInfo(schedulingFactoryInfoId);
                                schedulingFactoryInstance.setId(factoryInstanceIdRoller.getAndIncrement());
                                schedulingFactoryInstance.setSchedulingFactoryInfo(schedulingFactoryInfo);
                                schedulingFactoryInstance.setProducingLength(size);
                                schedulingFactoryInstance.setReapWindowSize(size);
                                schedulingFactoryInfo.appendFactoryInstance(schedulingFactoryInstance);
                                this.schedulingFactoryInstances.add(schedulingFactoryInstance);
                            } else {
                                Map<FieldFactoryInfoEntity, List<FieldFactoryEntity>> typeInstamceMap = fieldFactoryEntities
                                        .stream()
                                        .collect(Collectors.groupingBy(FieldFactoryEntity::getFieldFactoryInfoEntity));
                                typeInstamceMap.entrySet().forEach(entry -> {
                                    FieldFactoryInfoEntity type = entry.getKey();
                                    List<FieldFactoryEntity> instanceList = entry.getValue();
                                    for (int i = 0; i < instanceList.size(); i++) {
                                        FieldFactoryEntity fieldFactoryEntity = instanceList.get(i);
                                        SchedulingFactoryInstance schedulingFactoryInstance = new SchedulingFactoryInstance();
                                        SchedulingFactoryInfo.Id schedulingFactoryInfoId
                                                = SchedulingFactoryInfo.Id.of(type);
                                        SchedulingFactoryInfo schedulingFactoryInfo
                                                = buildOrGetSchedulingFactoryInfo(schedulingFactoryInfoId);
                                        schedulingFactoryInstance.setId(factoryInstanceIdRoller.getAndIncrement());
                                        schedulingFactoryInstance.setSeqNum(i + 1);
                                        schedulingFactoryInstance.setSchedulingFactoryInfo(schedulingFactoryInfo);
                                        schedulingFactoryInstance.setProducingLength(fieldFactoryEntity.getProducingLength());
                                        schedulingFactoryInstance.setReapWindowSize(fieldFactoryEntity.getReapWindowSize());
                                        schedulingFactoryInfo.appendFactoryInstance(schedulingFactoryInstance);
                                        this.schedulingFactoryInstances.add(schedulingFactoryInstance);
                                    }
                                });
                            }
                        }
                );

    }

    private void fetchAndMapToSchedulingWarehouse() {
        SchedulingWarehouse schedulingWarehouse = new SchedulingWarehouse();
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
        schedulingWarehouse.setProductAmountMap(productAmountMap);
        this.schedulingWarehouse = schedulingWarehouse;
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
                executionMode.setExecuteDuration(producingDuration);
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

    private SchedulingFactoryInfo buildOrGetSchedulingFactoryInfo(FieldFactoryInfoEntityProjectionForScheduling fieldFactoryInfoEntityDto) {
        SchedulingFactoryInfo schedulingFactoryInfo
                = buildOrGetSchedulingFactoryInfo(SchedulingFactoryInfo.Id.of(fieldFactoryInfoEntityDto));
        schedulingFactoryInfo.setCategoryName(fieldFactoryInfoEntityDto.getCategory());
        schedulingFactoryInfo.setLevel(fieldFactoryInfoEntityDto.getLevel());
        return schedulingFactoryInfo;
    }

    private Set<SchedulingProducingExecutionMode> calcProducingExecutionMode(
            ProductEntityProjectionForScheduling productDto,
            SchedulingProduct schedulingProduct
    ) {
        AtomicInteger idRoller = new AtomicInteger(1);
        Set<ProductManufactureInfoEntityProjection> productManufactureInfos = productDto.getManufactureInfoEntities();
        if (productManufactureInfos != null && !productManufactureInfos.isEmpty()) {
            Set<SchedulingProducingExecutionMode> executionModes = new LinkedHashSet<>();
            productManufactureInfos.forEach(productManufactureInfo -> {
                SchedulingProducingExecutionMode executionMode = new SchedulingProducingExecutionMode();
                executionMode.setId(idRoller.getAndIncrement());
                executionMode.setProduct(schedulingProduct);
                Duration producingDuration = productManufactureInfo.getProducingDuration();
                executionMode.setExecuteDuration(producingDuration);
                ProductAmountBill productAmountBill = new ProductAmountBill();
                executionMode.setMaterials(productAmountBill);
                Set<ProductMaterialsRelationProjection> productMaterialsRelations = productManufactureInfo.getProductMaterialsRelations();
                if (productMaterialsRelations != null && !productMaterialsRelations.isEmpty()) {
                    productMaterialsRelations.forEach(productMaterialsRelation -> {
                        ProductEntityProjectionJustId productId = productMaterialsRelation.getMaterial();
                        Integer amount = productMaterialsRelation.getAmount();
                        SchedulingProduct material
                                = buildOrGetSchedulingProduct(
                                SchedulingProduct.Id.of(productId)
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

    private SchedulingProduct buildOrGetSchedulingProduct(ProductEntityProjectionForScheduling productDto) {
        SchedulingProduct.Id productId = SchedulingProduct.Id.of(Objects.requireNonNull(productDto.getId()));
        SchedulingProduct schedulingProduct = buildOrGetSchedulingProduct(productId);
        schedulingProduct.setName(productDto.getName());
        schedulingProduct.setLevel(productDto.getLevel());
        return schedulingProduct;
    }

    private SchedulingProduct buildOrGetSchedulingProduct(ProductEntityProjectionJustId dk) {
        return buildOrGetSchedulingProduct(SchedulingProduct.Id.of(dk));
    }

    private SchedulingFactoryInfo buildOrGetSchedulingFactoryInfo(FieldFactoryInfoEntityDto fieldFactoryInfoEntityDto) {
        SchedulingFactoryInfo schedulingFactoryInfo
                = buildOrGetSchedulingFactoryInfo(SchedulingFactoryInfo.Id.of(fieldFactoryInfoEntityDto));
        schedulingFactoryInfo.setCategoryName(fieldFactoryInfoEntityDto.getCategory());
        schedulingFactoryInfo.setLevel(fieldFactoryInfoEntityDto.getLevel());
        return schedulingFactoryInfo;
    }

    private Set<SchedulingProducingExecutionMode> calcProducingExecutionMode(
            ProductEntityDtoForScheduling productDto,
            SchedulingProduct schedulingProduct
    ) {
        //deal producing info
        Set<SchedulingProducingExecutionMode> producingExecutionModes = new LinkedHashSet<>();
        Set<ProductManufactureInfoEntityDtoForScheduling> productManufactureInfoSet = productDto.getManufactureInfoEntities();
        for (ProductManufactureInfoEntityDtoForScheduling manufactureInfo : productManufactureInfoSet) {
            SchedulingProducingExecutionMode producingExecutionMode
                    = new SchedulingProducingExecutionMode();
            producingExecutionMode.setProduct(schedulingProduct);
            producingExecutionMode.setExecuteDuration(manufactureInfo.getProducingDuration());

            //deal materials info
            ProductAmountBill productAmountBill = new ProductAmountBill();
            Set<ProductMaterialsRelationDtoForScheduling> materialRelationSet = manufactureInfo.getProductMaterialsRelations();
            for (ProductMaterialsRelationDtoForScheduling materialRelation : materialRelationSet) {
                ProductEntityDtoJustId productEntityDtoJustId = materialRelation.getMaterial();
                Integer amount = materialRelation.getAmount();
                productAmountBill.put(
                        buildOrGetSchedulingProduct(SchedulingProduct.Id.of(productEntityDtoJustId)),
                        amount
                );
            }
            producingExecutionMode.setMaterials(productAmountBill);
            producingExecutionModes.add(producingExecutionMode);
            this.schedulingProducingExecutionModes.addAll(producingExecutionModes);
        }
        return producingExecutionModes;
    }

    private SchedulingProduct buildOrGetSchedulingProduct(ProductEntityDtoForScheduling productDto) {
        SchedulingProduct.Id productId = SchedulingProduct.Id.of(productDto.getId());
        SchedulingProduct schedulingProduct = buildOrGetSchedulingProduct(productId);
        schedulingProduct.setName(productDto.getName());
        schedulingProduct.setLevel(productDto.getLevel());
        return schedulingProduct;
    }

}
