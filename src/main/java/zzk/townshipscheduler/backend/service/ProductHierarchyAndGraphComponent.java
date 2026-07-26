package zzk.townshipscheduler.backend.service;

import io.arxila.javatuples.Pair;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.atteo.evo.inflector.English;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;
import zzk.townshipscheduler.backend.persistence.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.select.ProductEntityDtoForBuildUp;

import java.io.Serial;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProductHierarchyAndGraphComponent {

    public static final Pattern PATTERN = Pattern.compile("(\\d+)\\s+([^\\d\\s]+(?:\\s+[^\\d\\s]+)*)");

    public static final Pattern MULTIPLE_BOM_PATTERN = Pattern.compile("\\s\\bor\\b\\s");

    public static final JaroWinklerSimilarity JARO_WINKLER_SIMILARITY = new JaroWinklerSimilarity();

    private static final ProductHierarchyBuildUpContext BUILD_UP_CONTEXT = new ProductHierarchyBuildUpContext();

    private final ProductEntityRepository productEntityRepository;

    private final AtomicBoolean boolNeedCachedGoodHierarchiesReady = new AtomicBoolean(Boolean.TRUE);

    public ProductHierarchyAndGraphComponent(ProductEntityRepository productEntityRepository) {
        this.productEntityRepository = productEntityRepository;
    }

    public Set<ProductManufactureInfoEntity> calcManufactureInfoSet(ProductEntity productEntity) {
        Set<ContextProductHierarchyStructure> productHierarchies = calcProductsHierarchies(productEntity);
        List<Duration> durations = calcProductProducingDuration(productEntity);
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

    public Set<ContextProductHierarchyStructure> calcProductsHierarchies(ProductEntity productEntity) {
        if (boolNeedCachedGoodHierarchiesReady.get()) {
            calcProductsHierarchies();
        }

        return BUILD_UP_CONTEXT.resultByGroupInProduct(productEntity);
    }

    public void calcProductsHierarchies() {
        English.setMode(English.MODE.ENGLISH_CLASSICAL);

        Set<ProductEntity> productEntities
                = productEntityRepository.queryForRawProductHierarchyGraphBuilding(
                ProductEntity.class,
                Sort.by("id")
        );
        log.info("{} product entities load", productEntities.size());

        LinkedHashMap<String, ProductEntity> nameProductMap
                = new LinkedHashMap<>(productEntities.size());
        productEntities.forEach(
                productEntity -> {
                    //prepare material STRING process
                    nameProductMap.putIfAbsent(
                            productEntity.getNameForMaterial(),
                            productEntity
                    );

                    //prepare internal process
                    initProductIntoCachedRelations(productEntity);
                }
        );

        long systemCurrentTimeMillis = System.currentTimeMillis();
        log.info("calcGoodsHierarchies start");
        for (ProductEntity product : productEntities) {
            checkMaterialOfGoodsIntoHierarchy(
                    product,
                    nameProductMap
            );
        }

        //        for (ProductEntity currentProductDto : productEntities) {
        //            checkCompositeOfGoodsIntoHierarchy(
        //                    currentProductDto,
        //                    nameProductMap
        //            );
        //        }

        Set<ContextProductHierarchyStructure> hierarchyStructures = BUILD_UP_CONTEXT.resultByGroupInProduct();
        log.info(
                "calcGoodsHierarchies end...result in {} items,time(mill) {} passed",
                hierarchyStructures.size(),
                System.currentTimeMillis() - systemCurrentTimeMillis
        );

        boolNeedCachedGoodHierarchiesReady.set(Boolean.FALSE);
    }

    private void initProductIntoCachedRelations(
            ProductEntity productEntity
    ) {
        //        ContextProductHierarchyStructure manufactureRelation = ContextProductHierarchyStructure.builder()
        //                .id(idRoller.getAndIncrement())
        //                .productEntity(productEntity)
        //                .productId(ProductEntity.ProductId.of(productEntity.getId()))
        //                .composite(new ArrayList<>())
        //                .materials(new HashMap<>())
        //                .build();
        //        cachedRelations.add(manufactureRelation);
        BUILD_UP_CONTEXT.getGraph().addVertex(productEntity);
    }

    private void checkMaterialOfGoodsIntoHierarchy(
            ProductEntity product,
            LinkedHashMap<String, ProductEntity> nameProductMap
    ) {
        String bomStringFromEntity = product.getBomString();
        if (bomStringFromEntity == null || bomStringFromEntity.isEmpty()) {
            return;
        }

        if (MULTIPLE_BOM_PATTERN.matcher(bomStringFromEntity).find()) {
            String replacedOr = bomStringFromEntity.replaceAll(MULTIPLE_BOM_PATTERN.pattern(), ",");
            String[] split = replacedOr.split(",");

            List<String> materialStringSplitedList = Arrays.stream(split)
                    .filter(mayBomString -> !mayBomString.isEmpty())
                    .toList();

            for (int i = 0; i < materialStringSplitedList.size(); i++) {
                String bomString = materialStringSplitedList.get(i);
                Matcher matcher = PATTERN.matcher(bomString);
                while (matcher.find()) {
                    int quantity = refineQuantity(matcher);
                    ProductEntity materialProduct = refineMaterial(nameProductMap, matcher);

                    Graph<ProductEntity, ContextProductHierarchyGraphEdge> graph = BUILD_UP_CONTEXT.getGraph();
                    ContextProductHierarchyGraphEdge contextProductHierarchyGraphEdge = new ContextProductHierarchyGraphEdge(i + 1);
                    graph.addEdge(materialProduct, product, contextProductHierarchyGraphEdge);
                    graph.setEdgeWeight(contextProductHierarchyGraphEdge, quantity);
                }
            }
        } else {
            Matcher matcher = PATTERN.matcher(bomStringFromEntity);
            while (matcher.find()) {
                int quantity = refineQuantity(matcher);
                ProductEntity materialProduct = refineMaterial(nameProductMap, matcher);

                Graph<ProductEntity, ContextProductHierarchyGraphEdge> graph = BUILD_UP_CONTEXT.getGraph();
                ContextProductHierarchyGraphEdge contextProductHierarchyGraphEdge = new ContextProductHierarchyGraphEdge(1);
                graph.addEdge(materialProduct, product, contextProductHierarchyGraphEdge);
                graph.setEdgeWeight(contextProductHierarchyGraphEdge, quantity);
            }
        }

    }

    private int refineQuantity(Matcher matcher) {
        String rawQuantity = matcher.group(1);
        return Integer.parseInt(rawQuantity);
    }

    private ProductEntity refineMaterial(LinkedHashMap<String, ProductEntity> nameProductMap, Matcher matcher) {
        String rawMaterial = matcher.group(2).trim().toLowerCase();
        String materialProductName1 = English.plural(rawMaterial, 1);
        String materialProductName2 = jaroWinklerSimilarityLookup(
                rawMaterial,
                nameProductMap.keySet()
        );

        ProductEntity goodsMaterialDto = nameProductMap.get(materialProductName1);
        if (goodsMaterialDto == null) {
            goodsMaterialDto = nameProductMap.get(materialProductName2);
            if (goodsMaterialDto == null) {
                throw new IllegalStateException("unable to find %s".formatted(rawMaterial));
            }
        }
        return goodsMaterialDto;
    }

    private String jaroWinklerSimilarityLookup(String example, Set<String> candidates) {
        return candidates.stream()
                .map(str -> new Pair<String, Double>(
                        str,
                        JARO_WINKLER_SIMILARITY.apply(example, str)
                ))
                .filter(pair -> pair.value0() != null && pair.value1() != null)
                .max(Comparator.comparingDouble(Pair::value1))
                .map(Pair::value0)
                .get();
    }

//    private void checkCompositeOfGoodsIntoHierarchy(
//            ProductEntity productEntityDtoForBuildUp,
//            LinkedHashMap<String, ProductEntity> nameProductMap
//    ) {
//        Set<ContextProductHierarchyStructure> cachedRelations = BUILD_UP_CONTEXT.getCachedRelations();
//
//        Long productEntityId = productEntityDtoForBuildUp.getId();
//        String name = productEntityDtoForBuildUp.getName();
//
//        ArrayList<ProductEntity.ProductId> productIdList = cachedRelations.stream()
//                .filter(contextProductHierarchyStructure -> contextProductHierarchyStructure.getMaterials()
//                        .containsKey(ProductEntity.ProductId.of(productEntityId)))
//                .map(ContextProductHierarchyStructure::getProductId)
//                .collect(Collectors.toCollection(ArrayList::new));
//
//        cachedRelations.stream()
//                .filter(contextProductHierarchyStructure -> Objects.equals(
//                                contextProductHierarchyStructure.getProductId().getValue(),
//                                productEntityDtoForBuildUp.getId()
//                        )
//                )
//                .forEach(contextProductHierarchyStructure -> {
//                    contextProductHierarchyStructure.getComposite().addAll(productIdList);
//                });
//
//    }

    public List<Duration> calcProductProducingDuration(ProductEntity productEntity) {
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

    private static class ProductHierarchyBuildUpContext {

        @Getter
        public final Graph<ProductEntity, ContextProductHierarchyGraphEdge> graph
                = GraphTypeBuilder.<ProductEntity, ContextProductHierarchyGraphEdge>directed()
                .weighted(true)
                .allowingMultipleEdges(true)
                .allowingSelfLoops(true)
                .edgeClass(ContextProductHierarchyGraphEdge.class)
                .buildGraph();


        public Set<ContextProductHierarchyStructure> resultByGroupInProduct() {
            return graph.vertexSet().stream().map(this::resultByGroupInProduct).flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
        }

        public Set<ContextProductHierarchyStructure> resultByGroupInProduct(ProductEntity productEntity) {
            Set<ContextProductHierarchyGraphEdge> productHierarchyGraphEdges = graph.incomingEdgesOf(productEntity);
            return productHierarchyGraphEdges.stream()
                    .collect(
                            Collectors.collectingAndThen(
                                    Collectors.groupingBy(ContextProductHierarchyGraphEdge::getGroupId),
                                    integerListMap -> {
                                        Set<ContextProductHierarchyStructure> result = new LinkedHashSet<>();
                                        for (Map.Entry<Integer, List<ContextProductHierarchyGraphEdge>> entry : integerListMap.entrySet()) {
                                            Integer id = entry.getKey();
                                            List<ContextProductHierarchyGraphEdge> materialEdges = entry.getValue();
                                            Map<ProductEntity.ProductId, Integer> materialIdToAmountMap
                                                    = materialEdges.stream()
                                                    .collect(
                                                            Collectors.toMap(
                                                                    edge -> ProductEntity.ProductId.of(edge.getSource().getId()),
                                                                    ContextProductHierarchyGraphEdge::getAmount
                                                            )
                                                    );
                                            ContextProductHierarchyStructure hierarchyStructure = ContextProductHierarchyStructure.builder()
                                                    .id(id)
                                                    .productId(productEntity.getProductId())
                                                    .materials(materialIdToAmountMap)
                                                    .build();
                                            result.add(hierarchyStructure);
                                        }
                                        return result;
                                    }
                            )
                    );
        }

//        @Getter
//        private final Set<ContextProductHierarchyStructure> cachedRelations = new LinkedHashSet<>();

//        public Map<ProductEntity.ProductId, List<ContextProductHierarchyStructure>> resultByGroupInProduct() {
//            return cachedRelations.stream()
//                    .collect(Collectors.groupingBy(ContextProductHierarchyStructure::getProductId));
//        }

//        public Map<ProductEntity.ProductId, Integer> buildOrGetContextProductHierarchyStructure(ProductEntityDtoForBuildUp targetProduct, int splitLength, AtomicInteger idRoller) {
//            if (splitLength == 1) {
//                Optional<ContextProductHierarchyStructure> relationOptional
//                        = cachedRelations.stream()
//                        .filter(contextProductHierarchyStructure -> Objects.equals(
//                                contextProductHierarchyStructure.getProductId().getValue(),
//                                targetProduct.getId()
//                        ))
//                        .findFirst();
//                return relationOptional.map(ContextProductHierarchyStructure::getMaterials)
//                        .orElseThrow();
//            }
//            ContextProductHierarchyStructure manufactureRelation = ContextProductHierarchyStructure.builder()
//                    .id(idRoller.getAndIncrement())
//                    .productId(ProductEntity.ProductId.of(targetProduct.getId()))
//                    .composite(new ArrayList<>())
//                    .materials(new HashMap<>())
//                    .build();
//            cachedRelations.add(manufactureRelation);
//            return manufactureRelation.getMaterials();
//        }

    }

    @Data
    @Builder
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ContextProductHierarchyStructure {

        @EqualsAndHashCode.Include
        private int id;

        private ProductEntityDtoForBuildUp productEntity;

        @EqualsAndHashCode.Include
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

    @Getter
    public static class ContextProductHierarchyGraphEdge
            extends DefaultWeightedEdge {

        @Serial private static final long serialVersionUID = -1696195722093798307L;

        private final int groupId;

        public ContextProductHierarchyGraphEdge(int groupId) {
            this.groupId = groupId;
        }

        public ProductEntity.ProductId getSourceId() {
            return this.getSource().getProductId();
        }

        @Override
        public ProductEntity getSource() {
            return (ProductEntity) super.getSource();
        }

        public ProductEntity.ProductId getTargetId() {
            return this.getTarget().getProductId();
        }

        @Override
        public ProductEntity getTarget() {
            return (ProductEntity) super.getTarget();
        }

        public int getAmount() {
            return (int) this.getWeight();
        }

        @Override
        public double getWeight() {
            return super.getWeight();
        }

    }


}
