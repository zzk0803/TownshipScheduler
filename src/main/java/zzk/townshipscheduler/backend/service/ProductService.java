package zzk.townshipscheduler.backend.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.atteo.evo.inflector.English;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;
import zzk.townshipscheduler.backend.persistence.select.ProductEntityDtoForBuildUp;

import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductService {

    public static final String REGEX_MATERIAL_AMOUNT = "(\\d+)\\s+([^\\d\\s]+(?:\\s+[^\\d\\s]+)*)";

    private static final ProductHierarchyBuildUpContext CACHED_PRODUCT_HIERARCHY_BUILD_UP_CONTEXT
            = new ProductHierarchyBuildUpContext();

    private final ProductEntityRepository productEntityRepository;

    private boolean boolNeedCachedGoodHierarchiesReady = true;

    public ProductService(ProductEntityRepository productEntityRepository) {
        this.productEntityRepository = productEntityRepository;
    }

    //<editor-fold desc="delegate repository methods">
    public <T> Set<T> findBy(Class<T> projectionClass, Sort sort) {
        return productEntityRepository.findBy(projectionClass, sort);
    }

    public Optional<ProductEntity> queryById(Long id) {
        return productEntityRepository.queryById(id);
    }

    public Optional<ProductEntity> findByName(String name) {
        return productEntityRepository.findByName(name);
    }

    public List<FieldFactoryInfoEntity> queryFieldFactory() {
        return productEntityRepository.queryFieldFactory();
    }

    public Set<ProductEntity> queryForPrepareScheduling(Integer level) {
        return productEntityRepository.queryForPrepareScheduling(level);
    }

    public Optional<byte[]> queryProductImageById(Serializable id) {
        return productEntityRepository.queryProductImageById(id);
    }

    public Optional<byte[]> queryProductImageByName(String name) {
        return productEntityRepository.queryProductImageByName(name);
    }

    //</editor-fold>

    public Set<ProductManufactureInfoEntity> calcManufactureInfoSet(ProductEntity productEntity) {
        List<ContextProductHierarchyStructure> productHierarchies = calcGoodsHierarchies(productEntity);
        List<Duration> durations = calcGoodsDuration(productEntity);
        assert durations.size() == productHierarchies.size();

        Set<ProductManufactureInfoEntity> producingInfoSet = new HashSet<>();
        Iterator<ContextProductHierarchyStructure> manufactureRelationIterator = productHierarchies.iterator();
        Iterator<Duration> durationIterator = durations.iterator();
        while (manufactureRelationIterator.hasNext() || durationIterator.hasNext()) {
            ContextProductHierarchyStructure contextProductHierarchyStructure = null;
            try {
                contextProductHierarchyStructure = manufactureRelationIterator.next();
            } catch (NoSuchElementException e) {
                //don't be alert
            }

            Duration duration = null;
            try {
                duration = durationIterator.next();
            } catch (NoSuchElementException e) {
                //don't be alert
            }

            ProductManufactureInfoEntity productManufactureInfoEntity = buildProductManufactureInfoEntity(
                    productEntity,
                    contextProductHierarchyStructure,
                    duration
            );
            producingInfoSet.add(productManufactureInfoEntity);
        }

        return producingInfoSet;
    }

    public List<ContextProductHierarchyStructure> calcGoodsHierarchies(ProductEntity productEntity) {
        if (boolNeedCachedGoodHierarchiesReady) {
            calcGoodsHierarchies();
        }

        return CACHED_PRODUCT_HIERARCHY_BUILD_UP_CONTEXT.resultByGroupInProduct()
                .get(productEntity.getProductId());
    }

    public List<Duration> calcGoodsDuration(ProductEntity productEntity) {
        String durationString = productEntity.getDurationString();
        if (Objects.isNull(durationString)) {
            return Collections.singletonList(Duration.ZERO);
        }

        String replacedOr = durationString.replaceAll("\\s\\bor\\b\\s", ",");
        String replacedSpaceChar = replacedOr.replaceAll("\\s", "");
        String[] split = replacedSpaceChar.split(",");

        return Arrays.stream(split)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    String durationParseString = "";
                    try {
                        durationParseString = "PT" + s;
                        return Duration.parse(durationParseString);
                    } catch (Exception e) {
                        log.error(
                                "ERR! when parse product {} producing duration -> {}",
                                productEntity.getName(),
                                durationParseString
                        );
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    private ProductManufactureInfoEntity buildProductManufactureInfoEntity(
            ProductEntity productEntity,
            ContextProductHierarchyStructure contextProductHierarchyStructure,
            Duration duration
    ) {
        ProductManufactureInfoEntity productManufactureInfoEntity = new ProductManufactureInfoEntity();
        productManufactureInfoEntity.setProductEntity(productEntity);
        if (contextProductHierarchyStructure != null && !contextProductHierarchyStructure.boolAtomicProduct()) {
            Set<ProductMaterialsRelation> productMaterialsRelations = buildProductMaterialRelationSet(
                    productManufactureInfoEntity,
                    contextProductHierarchyStructure
            );
            productMaterialsRelations.forEach(productManufactureInfoEntity::attacheProductMaterialsRelation);
        }
        if (duration != null) {
            productManufactureInfoEntity.setProducingDuration(duration);
        } else {
            productManufactureInfoEntity.setProducingDuration(Duration.ZERO);
        }
        return productManufactureInfoEntity;
    }

    public void calcGoodsHierarchies() {
        Set<ContextProductHierarchyStructure> cachedRelations = CACHED_PRODUCT_HIERARCHY_BUILD_UP_CONTEXT.getCachedRelations();

        Set<ProductEntityDtoForBuildUp> productEntityDtoForBuildUpList = productEntityRepository.findBy(
                ProductEntityDtoForBuildUp.class,
                Sort.by("id")
        );
        log.info("{} product entities load", productEntityDtoForBuildUpList.size());

        AtomicInteger idRoller = new AtomicInteger(1);
        LinkedHashMap<String, ProductEntityDtoForBuildUp> nameProductDtoMap = new LinkedHashMap<>(
                productEntityDtoForBuildUpList.size());
        productEntityDtoForBuildUpList.forEach(
                productEntityDtoForBuildUp -> {
                    //prepare material STRING process
                    nameProductDtoMap.putIfAbsent(
                            productEntityDtoForBuildUp.getNameForMaterial(),
                            productEntityDtoForBuildUp
                    );

                    //prepare internal process
                    initProductIntoCachedRelations(cachedRelations, productEntityDtoForBuildUp, idRoller);
                }
        );

        long systemCurrentTimeMillis = System.currentTimeMillis();
        log.info("calcGoodsHierarchies start");
        for (ProductEntityDtoForBuildUp currentProductDto : productEntityDtoForBuildUpList) {
            checkMaterialOfGoodsIntoHierarchy(
                    currentProductDto,
                    nameProductDtoMap
            );
        }
        for (ProductEntityDtoForBuildUp currentProductDto : productEntityDtoForBuildUpList) {
            checkCompositeOfGoodsIntoHierarchy(
                    currentProductDto,
                    nameProductDtoMap
            );
        }

        Map<ProductEntity.ProductId, List<ContextProductHierarchyStructure>> groupedByProduct
                = CACHED_PRODUCT_HIERARCHY_BUILD_UP_CONTEXT.resultByGroupInProduct();
        log.info(
                "calcGoodsHierarchies end...result in {} items,{} passed",
                groupedByProduct.size(),
                System.currentTimeMillis() - systemCurrentTimeMillis
        );

        boolNeedCachedGoodHierarchiesReady = false;
    }

    private Set<ProductMaterialsRelation> buildProductMaterialRelationSet(
            ProductManufactureInfoEntity productManufactureInfoEntity,
            ContextProductHierarchyStructure contextProductHierarchyStructure
    ) {
        Set<ProductMaterialsRelation> productMaterialsRelations = new HashSet<>();
        Map<ProductEntity.ProductId, Integer> productManufactureRelationMaterials = contextProductHierarchyStructure.getMaterials();
        productManufactureRelationMaterials.forEach((productId, integer) -> {
            ProductMaterialsRelation productMaterialsRelation = new ProductMaterialsRelation();
            productMaterialsRelation.setProductManufactureInfo(productManufactureInfoEntity);
            productMaterialsRelation.setMaterial(productEntityRepository.getReferenceById(productId.getValue()));
            productMaterialsRelation.setAmount(integer);
            productMaterialsRelations.add(productMaterialsRelation);
        });
        return productMaterialsRelations;
    }

    private void initProductIntoCachedRelations(
            Set<ContextProductHierarchyStructure> cachedRelations,
            ProductEntityDtoForBuildUp productEntityDtoForBuildUp,
            AtomicInteger idRoller
    ) {
        ContextProductHierarchyStructure manufactureRelation = ContextProductHierarchyStructure.builder()
                .id(idRoller.getAndIncrement())
                .productId(ProductEntity.ProductId.of(productEntityDtoForBuildUp.getId()))
                .composite(new ArrayList<>())
                .materials(new HashMap<>())
                .build();
        cachedRelations.add(manufactureRelation);
    }

    private void checkMaterialOfGoodsIntoHierarchy(
            ProductEntityDtoForBuildUp targetProduct,
            LinkedHashMap<String, ProductEntityDtoForBuildUp> nameProductMap
    ) {
        String bomStringFromEntity = targetProduct.getBomString();
        if (bomStringFromEntity == null || bomStringFromEntity.isEmpty()) {
            return;
        }

        String replacedOr = bomStringFromEntity.replaceAll("\\s\\bor\\b\\s", ",");
        String[] split = replacedOr.split(",");

        Pattern pattern = Pattern.compile(REGEX_MATERIAL_AMOUNT);

        Arrays.stream(split)
                .filter(mayBomString -> !mayBomString.isEmpty() || !mayBomString.isBlank())
                .forEach(bomString -> {
                    Matcher matcher = pattern.matcher(bomString);
                    while (matcher.find()) {
                        try {
                            int quantity = Integer.parseInt(matcher.group(1));
                            String productName = English.plural(matcher.group(2).trim(), 1);

                            ProductEntityDtoForBuildUp goodsMaterialDto = nameProductMap.get(productName);
                            assert goodsMaterialDto != null;

                            Optional<ContextProductHierarchyStructure> relationOptional = CACHED_PRODUCT_HIERARCHY_BUILD_UP_CONTEXT.getCachedRelations()
                                    .stream()
                                    .filter(contextProductHierarchyStructure -> Objects.equals(
                                            contextProductHierarchyStructure.getProductId().getValue(),
                                            targetProduct.getId()
                                    ))
                                    .findFirst();
                            Map<ProductEntity.ProductId, Integer> materials = relationOptional.map(
                                            ContextProductHierarchyStructure::getMaterials)
                                    .orElseThrow();
                            materials.putIfAbsent(ProductEntity.ProductId.of(goodsMaterialDto.getId()), quantity);

                        } catch (RuntimeException e) {
                            //ignore and do nothing
                        }
                    }
                });
    }

    private void checkCompositeOfGoodsIntoHierarchy(
            ProductEntityDtoForBuildUp productEntityDtoForBuildUp,
            LinkedHashMap<String, ProductEntityDtoForBuildUp> nameProductMap
    ) {
        Set<ContextProductHierarchyStructure> cachedRelations = CACHED_PRODUCT_HIERARCHY_BUILD_UP_CONTEXT.getCachedRelations();

        Long productEntityId = productEntityDtoForBuildUp.getId();
        String name = productEntityDtoForBuildUp.getName();

        ArrayList<ProductEntity.ProductId> productIdList = cachedRelations.stream()
                .filter(contextProductHierarchyStructure -> contextProductHierarchyStructure.getMaterials()
                        .containsKey(ProductEntity.ProductId.of(productEntityId)))
                .map(ContextProductHierarchyStructure::getProductId)
                .collect(Collectors.toCollection(ArrayList::new));

        cachedRelations.stream()
                .filter(contextProductHierarchyStructure -> Objects.equals(
                                contextProductHierarchyStructure.getProductId().getValue(),
                                productEntityDtoForBuildUp.getId()
                        )
                )
                .forEach(contextProductHierarchyStructure -> {
                    contextProductHierarchyStructure.getComposite().addAll(productIdList);
                });

    }

    private static class ProductHierarchyBuildUpContext {

        private final Set<ContextProductHierarchyStructure> cachedRelations = new LinkedHashSet<>();

        public Set<ContextProductHierarchyStructure> getCachedRelations() {
            return cachedRelations;
        }

        public Map<ProductEntity.ProductId, List<ContextProductHierarchyStructure>> resultByGroupInProduct() {
            return cachedRelations.stream()
                    .collect(Collectors.groupingBy(ContextProductHierarchyStructure::getProductId));
        }

    }

    @Data
    @Builder
    public static class ContextProductHierarchyStructure {

        private int id;

        private ProductEntity.ProductId productId;

        private List<ProductEntity.ProductId> composite;

        private Map<ProductEntity.ProductId, Integer> materials;

        private Boolean atomicProduct;

        public boolean boolAtomicProduct() {
            if (atomicProduct == null) {
                atomicProduct = materials == null || materials.isEmpty();
            }
            return atomicProduct;
        }

    }


}
