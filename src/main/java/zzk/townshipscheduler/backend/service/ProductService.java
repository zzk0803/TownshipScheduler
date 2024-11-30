package zzk.townshipscheduler.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.atteo.evo.inflector.English;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.ProductId;
import zzk.townshipscheduler.backend.TempProductManufactureRelation;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.persistence.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.ProductManufactureInfoEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.ProductMaterialsRelationRepository;
import zzk.townshipscheduler.pojo.projection.ProductEntityDto;

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

    private static final GoodsHierarchyContext cachedGoodsHierarchyContext = new GoodsHierarchyContext();

    private final ProductEntityRepository productEntityRepository;

    private final ProductMaterialsRelationRepository productMaterialsRelationRepository;

    private final ProductManufactureInfoEntityRepository productManufactureInfoEntityRepository;

    private boolean boolNeedCachedGoodHierarchiesReady = true;

    public ProductService(
            ProductEntityRepository productEntityRepository,
            ProductMaterialsRelationRepository productMaterialsRelationRepository,
            ProductManufactureInfoEntityRepository productManufactureInfoEntityRepository
    ) {
        this.productEntityRepository = productEntityRepository;
        this.productMaterialsRelationRepository = productMaterialsRelationRepository;
        this.productManufactureInfoEntityRepository = productManufactureInfoEntityRepository;
    }

    //<editor-fold desc="delegate repository methods">
    //</editor-fold>

    public Set<ProductManufactureInfoEntity> calcManufactureInfoSet(ProductEntity productEntity) {
        List<TempProductManufactureRelation> tempProductManufactureRelations = calcGoodsHierarchies(productEntity);
        List<Duration> durations = calcGoodsDuration(productEntity);
        assert durations.size() == tempProductManufactureRelations.size();

        Set<ProductManufactureInfoEntity> producingInfoSet = new HashSet<>();
        Iterator<TempProductManufactureRelation> manufactureRelationIterator = tempProductManufactureRelations.iterator();
        Iterator<Duration> durationIterator = durations.iterator();
        while (manufactureRelationIterator.hasNext() || durationIterator.hasNext()) {
            TempProductManufactureRelation tempProductManufactureRelation = null;
            try {
                tempProductManufactureRelation = manufactureRelationIterator.next();
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
                    tempProductManufactureRelation,
                    duration
            );
            producingInfoSet.add(productManufactureInfoEntity);
        }

        return producingInfoSet;
    }

    private ProductManufactureInfoEntity buildProductManufactureInfoEntity(
            ProductEntity productEntity,
            TempProductManufactureRelation tempProductManufactureRelation,
            Duration duration
    ) {
        ProductManufactureInfoEntity productManufactureInfoEntity = new ProductManufactureInfoEntity();
        productManufactureInfoEntity.setProductEntity(productEntity);
        if (tempProductManufactureRelation != null && !tempProductManufactureRelation.boolAtomicProduct()) {
            Set<ProductMaterialsRelation> productMaterialsRelations = buildProductMaterialRelationSet(
                    productManufactureInfoEntity,
                    tempProductManufactureRelation
            );
            productMaterialsRelations.forEach(productManufactureInfoEntity::attacheProductMaterialsRelation);
        }
        if (duration != null) {
            productManufactureInfoEntity.setProducingDuration(duration);
        }
        return productManufactureInfoEntity;
    }

    private Set<ProductMaterialsRelation> buildProductMaterialRelationSet(
            ProductManufactureInfoEntity productManufactureInfoEntity,
            TempProductManufactureRelation tempProductManufactureRelation
    ) {
        Set<ProductMaterialsRelation> productMaterialsRelations = new HashSet<>();
        Map<ProductId, Integer> productManufactureRelationMaterials = tempProductManufactureRelation.getMaterials();
        productManufactureRelationMaterials.forEach((productId, integer) -> {
            ProductMaterialsRelation productMaterialsRelation = new ProductMaterialsRelation();
            productMaterialsRelation.setProductManufactureInfo(productManufactureInfoEntity);
            productMaterialsRelation.setMaterial(productEntityRepository.getReferenceById(productId.getValue()));
            productMaterialsRelation.setAmount(integer);
            productMaterialsRelations.add(productMaterialsRelation);
        });
        return productMaterialsRelations;
    }

    public List<TempProductManufactureRelation> calcGoodsHierarchies(ProductEntity productEntity) {
        if (boolNeedCachedGoodHierarchiesReady) {
            calcGoodsHierarchies();
        }

        return cachedGoodsHierarchyContext.resultByGroupInProduct().get(ProductId.of(productEntity.getId()));
    }

    public void calcGoodsHierarchies() {
        Set<TempProductManufactureRelation> cachedRelations = cachedGoodsHierarchyContext.getCachedRelations();

        List<ProductEntityDto> productEntityDtoList = productEntityRepository.findBy(
                ProductEntityDto.class,
                Sort.by("id")
        );
        log.info("{} product entities load", productEntityDtoList.size());

        AtomicInteger idRoller = new AtomicInteger(1);
        LinkedHashMap<String, ProductEntityDto> nameProductDtoMap = new LinkedHashMap<>(productEntityDtoList.size());
        productEntityDtoList.forEach(
                productEntityDto -> {
                    //prepare material STRING process
                    nameProductDtoMap.putIfAbsent(
                            productEntityDto.getNameForMaterial(),
                            productEntityDto
                    );

                    //prepare internal process
                    initProductIntoCachedRelations(cachedRelations, productEntityDto, idRoller);
                }
        );

        long systemCurrentTimeMillis = System.currentTimeMillis();
        log.info("calcGoodsHierarchies start");
        for (ProductEntityDto currentProductDto : productEntityDtoList) {
            checkMaterialOfGoodsIntoHierarchy(
                    currentProductDto,
                    nameProductDtoMap
            );
        }
        for (ProductEntityDto currentProductDto : productEntityDtoList) {
            checkCompositeOfGoodsIntoHierarchy(
                    currentProductDto,
                    nameProductDtoMap
            );
        }

        Map<ProductId, List<TempProductManufactureRelation>> groupedByProduct = cachedGoodsHierarchyContext.resultByGroupInProduct();
        log.info(
                "calcGoodsHierarchies end...result in {} items,{} passed",
                groupedByProduct.size(),
                System.currentTimeMillis() - systemCurrentTimeMillis
        );

        boolNeedCachedGoodHierarchiesReady = false;
    }

    private void initProductIntoCachedRelations(
            Set<TempProductManufactureRelation> cachedRelations,
            ProductEntityDto productEntityDto,
            AtomicInteger idRoller
    ) {
        TempProductManufactureRelation manufactureRelation = TempProductManufactureRelation.builder()
                .id(idRoller.getAndIncrement())
                .productId(ProductId.of(productEntityDto.getId()))
                .composite(new ArrayList<>())
                .materials(new HashMap<>())
                .build();
        cachedRelations.add(manufactureRelation);
    }

    private void checkMaterialOfGoodsIntoHierarchy(
            ProductEntityDto targetProduct,
            LinkedHashMap<String, ProductEntityDto> nameProductMap
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

                            ProductEntityDto goodsMaterialDto = nameProductMap.get(productName);
                            assert goodsMaterialDto != null;

                            Optional<TempProductManufactureRelation> relationOptional = cachedGoodsHierarchyContext.getCachedRelations()
                                    .stream()
                                    .filter(tempProductManufactureRelation -> Objects.equals(
                                            tempProductManufactureRelation.getProductId().getValue(),
                                            targetProduct.getId()
                                    ))
                                    .findFirst();
                            Map<ProductId, Integer> materials = relationOptional.map(TempProductManufactureRelation::getMaterials)
                                    .orElseThrow();
                            materials.putIfAbsent(ProductId.of(goodsMaterialDto.getId()), quantity);

                        } catch (RuntimeException e) {
                            //ignore and do nothing
                        }
                    }
                });
    }

    private void checkCompositeOfGoodsIntoHierarchy(
            ProductEntityDto productEntityDto,
            LinkedHashMap<String, ProductEntityDto> nameProductMap
    ) {
        Set<TempProductManufactureRelation> cachedRelations = cachedGoodsHierarchyContext.getCachedRelations();

        Long productEntityId = productEntityDto.getId();
        String name = productEntityDto.getName();

        ArrayList<ProductId> productIdList = cachedRelations.stream()
                .filter(tempProductManufactureRelation -> tempProductManufactureRelation.getMaterials()
                        .containsKey(ProductId.of(productEntityId)))
                .map(TempProductManufactureRelation::getProductId)
                .collect(Collectors.toCollection(ArrayList::new));

        cachedRelations.stream()
                .filter(tempProductManufactureRelation -> Objects.equals(
                                tempProductManufactureRelation.getProductId().getValue(),
                                productEntityDto.getId()
                        )
                )
                .forEach(tempProductManufactureRelation -> {
                    tempProductManufactureRelation.getComposite().addAll(productIdList);
                });

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

    private static class GoodsHierarchyContext {

        private final Set<TempProductManufactureRelation> cachedRelations = new LinkedHashSet<>();

        public Set<TempProductManufactureRelation> getCachedRelations() {
            return cachedRelations;
        }

        public Map<ProductId, List<TempProductManufactureRelation>> resultByGroupInProduct() {
            return cachedRelations.stream()
                    .collect(Collectors.groupingBy(TempProductManufactureRelation::getProductId));
        }

    }

}
