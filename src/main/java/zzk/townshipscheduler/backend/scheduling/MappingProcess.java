package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.persistence.select.*;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class MappingProcess {

    private final TownshipSchedulingRequest townshipSchedulingRequest;

    private final Map<SchedulingProduct.Id, SchedulingProduct> idProductMap;

    private final Map<SchedulingFactoryInfo.Id, SchedulingFactoryInfo> idFactoryMap;

    private final Set<SchedulingGameActionExecutionMode> schedulingGameActionExecutionModes;

    private final Set<SchedulingFactoryInstance> schedulingFactoryInstances;

    private final Set<SchedulingOrder> schedulingOrders;

    private SchedulingWarehouse schedulingWarehouse;

    public MappingProcess(TownshipSchedulingRequest townshipSchedulingRequest) {
        this.townshipSchedulingRequest = townshipSchedulingRequest;
        this.idProductMap = new LinkedHashMap<>();
        this.idFactoryMap = new LinkedHashMap<>();
        this.schedulingGameActionExecutionModes = new LinkedHashSet<>();
        this.schedulingFactoryInstances = new LinkedHashSet<>();
        this.schedulingOrders = new LinkedHashSet<>();
    }

    public TownshipSchedulingProblem map() {
        doMapping();
        TownshipSchedulingProblem schedulingProblem
                = new TownshipSchedulingProblem(
                new LinkedHashSet<>(this.idProductMap.values()),
                new LinkedHashSet<>(this.idFactoryMap.values()),
                this.schedulingOrders,
                this.schedulingFactoryInstances,
                this.schedulingWarehouse,
                new SchedulingWorkTimeLimit(
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(3)
                )
        );
        schedulingProblem.setupGameActions();

        return schedulingProblem;
    }


    private void doMapping() {
        fetchAndMapToSchedulingProduct();
        fetchAndMapToSchedulingFactoryInfo();
        fetchAndMapToSchedulingFactoryInstance();
        fetchAndMapToSchedulingWarehouse();
        fetchAndMapToSchedulingOrder();
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

    private SchedulingFactoryInfo buildOrGetSchedulingFactoryInfo(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
        SchedulingFactoryInfo schedulingFactoryInfo
                = buildOrGetSchedulingFactoryInfo(SchedulingFactoryInfo.Id.of(fieldFactoryInfoEntity));
        schedulingFactoryInfo.setCategoryName(fieldFactoryInfoEntity.getCategory());
        schedulingFactoryInfo.setLevel(fieldFactoryInfoEntity.getLevel());
        return schedulingFactoryInfo;
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

            Set<SchedulingGameActionExecutionMode> producingExecutionModes
                    = calcProducingExecutionMode(productDto, schedulingProduct);
            schedulingProduct.setProducingExecutionModeSet(producingExecutionModes);
            this.schedulingGameActionExecutionModes.addAll(producingExecutionModes);
        }
    }

    private Set<SchedulingGameActionExecutionMode> calcProducingExecutionMode(
            ProductEntity productEntity,
            SchedulingProduct schedulingProduct
    ) {
        AtomicInteger idRoller = new AtomicInteger(1);
        Set<ProductManufactureInfoEntity> productManufactureInfos = productEntity.getManufactureInfoEntities();
        if (productManufactureInfos != null && !productManufactureInfos.isEmpty()) {
            Set<SchedulingGameActionExecutionMode> executionModes = new LinkedHashSet<>();
            productManufactureInfos.forEach(productManufactureInfo -> {
                SchedulingGameActionExecutionMode executionMode = new SchedulingGameActionExecutionMode();
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
            SchedulingGameActionExecutionMode defaultProducingExecutionMode = new SchedulingGameActionExecutionMode();
            defaultProducingExecutionMode.setId(idRoller.getAndIncrement());
            defaultProducingExecutionMode.setProduct(schedulingProduct);
            return Set.of(defaultProducingExecutionMode);
        }

    }

    private SchedulingProduct buildOrGetSchedulingProduct(ProductEntity product) {
        SchedulingProduct.Id productId = SchedulingProduct.Id.of(Objects.requireNonNull(product.getId()));
        SchedulingProduct schedulingProduct = buildOrGetSchedulingProduct(productId);
        schedulingProduct.setName(product.getName());
        schedulingProduct.setLevel(product.getLevel());
        return schedulingProduct;

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

    private SchedulingFactoryInfo buildOrGetSchedulingFactoryInfo(SchedulingFactoryInfo.Id schedulingFactoryInfoId) {
        if (!idFactoryMap.containsKey(schedulingFactoryInfoId)) {
            SchedulingFactoryInfo schedulingFactoryInfo = new SchedulingFactoryInfo();
            schedulingFactoryInfo.setId(schedulingFactoryInfoId);
            idFactoryMap.put(schedulingFactoryInfoId, schedulingFactoryInfo);
        }
        return idFactoryMap.get(schedulingFactoryInfoId);

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
                            buildOrGetSchedulingProduct(SchedulingProduct.Id.of(product.getId())),
                            amount
                    );
                }
        );
        return productAmountBill;
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

    private void fetchAndMapToSchedulingFactoryInstance() {
        townshipSchedulingRequest.getPlayerEntityFieldFactoryEntities()
                .stream()
                .map(fieldFactoryEntity -> {
                    SchedulingFactoryInstance schedulingFactoryInstance = new SchedulingFactoryInstance();
                    SchedulingFactoryInfo.Id schedulingFactoryInfoId
                            = SchedulingFactoryInfo.Id.of(fieldFactoryEntity.getFieldFactoryInfoEntity());
                    SchedulingFactoryInfo schedulingFactoryInfo
                            = buildOrGetSchedulingFactoryInfo(schedulingFactoryInfoId);
                    schedulingFactoryInstance.setSchedulingFactoryInfo(schedulingFactoryInfo);
                    schedulingFactoryInstance.setProducingLength(fieldFactoryEntity.getProducingLength());
                    schedulingFactoryInstance.setReapWindowSize(fieldFactoryEntity.getReapWindowSize());
                    return schedulingFactoryInstance;
                })
                .forEach(this.schedulingFactoryInstances::add);
    }

    private SchedulingFactoryInfo buildOrGetSchedulingFactoryInfo(FieldFactoryInfoEntityProjectionForScheduling fieldFactoryInfoEntityDto) {
        SchedulingFactoryInfo schedulingFactoryInfo
                = buildOrGetSchedulingFactoryInfo(SchedulingFactoryInfo.Id.of(fieldFactoryInfoEntityDto));
        schedulingFactoryInfo.setCategoryName(fieldFactoryInfoEntityDto.getCategory());
        schedulingFactoryInfo.setLevel(fieldFactoryInfoEntityDto.getLevel());
        return schedulingFactoryInfo;
    }

    private Set<SchedulingGameActionExecutionMode> calcProducingExecutionMode(
            ProductEntityProjectionForScheduling productDto,
            SchedulingProduct schedulingProduct
    ) {
        AtomicInteger idRoller = new AtomicInteger(1);
        Set<ProductManufactureInfoEntityProjection> productManufactureInfos = productDto.getManufactureInfoEntities();
        if (productManufactureInfos != null && !productManufactureInfos.isEmpty()) {
            Set<SchedulingGameActionExecutionMode> executionModes = new LinkedHashSet<>();
            productManufactureInfos.forEach(productManufactureInfo -> {
                SchedulingGameActionExecutionMode executionMode = new SchedulingGameActionExecutionMode();
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
            SchedulingGameActionExecutionMode defaultProducingExecutionMode = new SchedulingGameActionExecutionMode();
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

    private Set<SchedulingGameActionExecutionMode> calcProducingExecutionMode(
            ProductEntityDtoForScheduling productDto,
            SchedulingProduct schedulingProduct
    ) {
        //deal producing info
        Set<SchedulingGameActionExecutionMode> producingExecutionModes = new LinkedHashSet<>();
        Set<ProductManufactureInfoEntityDtoForScheduling> productManufactureInfoSet = productDto.getManufactureInfoEntities();
        for (ProductManufactureInfoEntityDtoForScheduling manufactureInfo : productManufactureInfoSet) {
            SchedulingGameActionExecutionMode producingExecutionMode
                    = new SchedulingGameActionExecutionMode();
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
            this.schedulingGameActionExecutionModes.addAll(producingExecutionModes);
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
