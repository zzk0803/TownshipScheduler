package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

class MappingProcess {

    private final TownshipSchedulingRequest townshipSchedulingRequest;

    private final Map<SchedulingProduct.Id, SchedulingProduct> idProductMap;

    private final Map<SchedulingFactoryInfo.Id, SchedulingFactoryInfo> idFactoryMap;

    private final Set<SchedulingProducingExecutionMode> schedulingProducingExecutionModes;

    private final Set<SchedulingFactoryInstance> schedulingFactoryInstances;

    private final Set<SchedulingOrder> schedulingOrders;

    private SchedulingWarehouse schedulingWarehouse;

    public MappingProcess(TownshipSchedulingRequest townshipSchedulingRequest) {
        this.townshipSchedulingRequest = townshipSchedulingRequest;
        this.idProductMap = new LinkedHashMap<>();
        this.idFactoryMap = new LinkedHashMap<>();
        this.schedulingProducingExecutionModes = new LinkedHashSet<>();
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
        schedulingProblem.setupDateTimeSlotSet();
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
        Set<FieldFactoryInfoEntityProjectionForScheduling> factoryInfoEntities = townshipSchedulingRequest.getFieldFactoryInfoEntities();
//            Set<FieldFactoryInfoEntityDto> factoryInfoEntities = townshipSchedulingRequest.getFieldFactoryInfoEntities();
        for (FieldFactoryInfoEntityProjectionForScheduling factoryDto : factoryInfoEntities) {
//            for (FieldFactoryInfoEntityDto factoryDto : factoryInfoEntities) {
            SchedulingFactoryInfo schedulingFactoryInfo = buildOrGetSchedulingFactoryInfo(factoryDto);
            factoryDto.getPortfolioGoods().stream()
                    .map(productEntityDtoJustId -> this.buildOrGetSchedulingProduct(
                            SchedulingProduct.Id.of(productEntityDtoJustId.getId())
                    ))
                    .forEach(schedulingFactoryInfo::appendPortfolioProduct);
            schedulingFactoryInfo.setProducingType(factoryDto.getProducingType().name());
            schedulingFactoryInfo.setDefaultInstanceAmount(factoryDto.getDefaultInstanceAmount());
            schedulingFactoryInfo.setDefaultProducingCapacity(factoryDto.getDefaultProducingCapacity());
            schedulingFactoryInfo.setDefaultReapWindowCapacity(factoryDto.getDefaultReapWindowCapacity());
            schedulingFactoryInfo.setMaxInstanceAmount(factoryDto.getMaxInstanceAmount());
            schedulingFactoryInfo.setMaxProducingCapacity(factoryDto.getMaxProducingCapacity());
            schedulingFactoryInfo.setMaxReapWindowCapacity(factoryDto.getMaxReapWindowCapacity());
        }
    }

    private SchedulingFactoryInfo buildOrGetSchedulingFactoryInfo(FieldFactoryInfoEntityProjectionForScheduling fieldFactoryInfoEntityDto) {
        SchedulingFactoryInfo schedulingFactoryInfo
                = buildOrGetSchedulingFactoryInfo(SchedulingFactoryInfo.Id.of(fieldFactoryInfoEntityDto));
        schedulingFactoryInfo.setCategoryName(fieldFactoryInfoEntityDto.getCategory());
        schedulingFactoryInfo.setLevel(fieldFactoryInfoEntityDto.getLevel());
        return schedulingFactoryInfo;
    }

    private void fetchAndMapToSchedulingProduct() {
        Set<ProductEntityProjectionForScheduling> productDtoList = townshipSchedulingRequest.getProductEntities();
        SchedulingProduct schedulingProduct = null;
        for (ProductEntityProjectionForScheduling productDto : productDtoList) {
            schedulingProduct = buildOrGetSchedulingProduct(productDto);

            FieldFactoryInfoEntityProjectionForScheduling fieldFactoryInfo
                    = productDto.getFieldFactoryInfo();
            SchedulingFactoryInfo schedulingFactoryInfo
                    = buildOrGetSchedulingFactoryInfo(
                            SchedulingFactoryInfo.Id.of(fieldFactoryInfo)
            );
            schedulingProduct.setRequireFactory(schedulingFactoryInfo);

            Set<SchedulingProducingExecutionMode> producingExecutionModes
                    = calcProducingExecutionMode(productDto, schedulingProduct);
            schedulingProduct.setProducingExecutionModeSet(producingExecutionModes);
            this.schedulingProducingExecutionModes.addAll(producingExecutionModes);
        }
    }

    private Set<SchedulingProducingExecutionMode> calcProducingExecutionMode(
            ProductEntityProjectionForScheduling productDto,
            SchedulingProduct schedulingProduct
    ) {
        Set<ProductManufactureInfoEntityProjection> productManufactureInfos = productDto.getManufactureInfoEntities();
        if (productManufactureInfos != null && !productManufactureInfos.isEmpty()) {
            Set<SchedulingProducingExecutionMode> executionModes = new LinkedHashSet<>();
            productManufactureInfos.forEach(productManufactureInfo -> {
                SchedulingProducingExecutionMode executionMode = new SchedulingProducingExecutionMode();
                executionMode.setUuid(UUID.randomUUID().toString());
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
            });
            return executionModes;
        } else {
            SchedulingProducingExecutionMode defaultProducingExecutionMode = new SchedulingProducingExecutionMode();
            defaultProducingExecutionMode.setUuid(UUID.randomUUID().toString());
            defaultProducingExecutionMode.setProduct(schedulingProduct);
            return Set.of(defaultProducingExecutionMode);
        }
    }

    private SchedulingFactoryInfo buildOrGetSchedulingFactoryInfo(SchedulingFactoryInfo.Id schedulingFactoryInfoId) {
        if (!idFactoryMap.containsKey(schedulingFactoryInfoId)) {
            SchedulingFactoryInfo schedulingFactoryInfo = new SchedulingFactoryInfo();
            schedulingFactoryInfo.setId(schedulingFactoryInfoId);
            idFactoryMap.put(schedulingFactoryInfoId, schedulingFactoryInfo);
        }
        return idFactoryMap.get(schedulingFactoryInfoId);

    }

    private SchedulingProduct buildOrGetSchedulingProduct(ProductEntityProjectionForScheduling productDto) {
        SchedulingProduct.Id productId = SchedulingProduct.Id.of(Objects.requireNonNull(productDto.getId()));
        SchedulingProduct schedulingProduct = buildOrGetSchedulingProduct(productId);
        schedulingProduct.setName(productDto.getName());
        schedulingProduct.setLevel(productDto.getLevel());
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

    private void fetchAndMapToSchedulingOrder() {
        townshipSchedulingRequest.getPlayerEntityOrderEntities()
                .forEach(orderDto -> {
                    Long id = orderDto.getId();
                    String orderType = orderDto.getOrderType().name();
                    LocalDateTime deadLine = orderDto.getDeadLine();
                    ProductAmountBill productAmountBill = createProductAmountBill(orderDto);
                    this.schedulingOrders.add(
                            new SchedulingOrder(
                                    id,
                                    productAmountBill,
                                    orderType,
                                    deadLine
                            ));
                });
    }

    private ProductAmountBill createProductAmountBill(OrderEntityProjection orderDto) {
        Map<ProductEntity, Integer> productIdAmountMap = orderDto.getProductAmountMap();
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

        WarehouseEntityProjection warehouseEntityProjection
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
                .map(fieldFactoryEntityDto -> {
                    SchedulingFactoryInstance schedulingFactoryInstance = new SchedulingFactoryInstance();
                    SchedulingFactoryInfo.Id schedulingFactoryInfoId
                            = SchedulingFactoryInfo.Id.of(fieldFactoryEntityDto);
                    SchedulingFactoryInfo schedulingFactoryInfo = buildOrGetSchedulingFactoryInfo(
                            schedulingFactoryInfoId);
                    schedulingFactoryInstance.setFactoryInfo(schedulingFactoryInfo);
                    schedulingFactoryInstance.setProducingLength(fieldFactoryEntityDto.getProducingLength());
                    return schedulingFactoryInstance;
                })
                .forEach(this.schedulingFactoryInstances::add);
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
