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

    public static final Pattern AMOUNT_MATERIAL_PATTERN = Pattern.compile("(\\d+)\\s+([^\\d\\s]+(?:\\s+[^\\d\\s]+)*)");

    public static final Pattern MULTIPLE_APPROCH_BOM_PATTERN = Pattern.compile("\\s\\bor\\b\\s");

    public static final JaroWinklerSimilarity JARO_WINKLER_SIMILARITY = new JaroWinklerSimilarity();

    private static final ProductHierarchyBuildUpContext BUILD_UP_CONTEXT = new ProductHierarchyBuildUpContext();

    private final ProductEntityRepository productEntityRepository;

    private final AtomicBoolean boolNeedCachedGoodHierarchiesReady = new AtomicBoolean(Boolean.TRUE);

    public ProductHierarchyAndGraphComponent(ProductEntityRepository productEntityRepository) {
        this.productEntityRepository = productEntityRepository;
    }

    public Set<ProductManufactureInfoEntity> calcManufactureInfoSet(ProductEntity productEntity) {
        List<ContextProductHierarchyStructure> productHierarchies = calcProductsHierarchies(productEntity);
        List<Duration> productDurations = calcProductProducingDuration(productEntity);
        int productHierarchiesSize = productHierarchies.size();
        int productDurationsSize = productDurations.size();
        if (productHierarchiesSize != productDurationsSize) {
            log.warn("product {} materialsSize != durationSize", productEntity.getName());
        }
        int iteratingSize = Math.min(productHierarchiesSize, productDurationsSize);

        Set<ProductManufactureInfoEntity> producingInfoSet = new HashSet<>();
        for (int i = 0; i < iteratingSize; i++) {
            ContextProductHierarchyStructure contextProductHierarchyStructure = productHierarchies.get(i);
            Duration duration = productDurations.get(i);
            producingInfoSet.add(
                    buildProductManufactureInfoEntity(
                            contextProductHierarchyStructure,
                            duration
                    )
            );
        }

        return producingInfoSet;
    }

    public List<ContextProductHierarchyStructure> calcProductsHierarchies(ProductEntity productEntity) {
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
                    BUILD_UP_CONTEXT.getGraph()
                            .addVertex(productEntity);
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

        Set<ContextProductHierarchyStructure> hierarchyStructures = BUILD_UP_CONTEXT.resultByGroupInProduct();
        log.info(
                "calcGoodsHierarchies end...result in {} items,time(mill) {} passed",
                hierarchyStructures.size(),
                System.currentTimeMillis() - systemCurrentTimeMillis
        );

        boolNeedCachedGoodHierarchiesReady.set(Boolean.FALSE);
    }

    private void checkMaterialOfGoodsIntoHierarchy(
            ProductEntity product,
            LinkedHashMap<String, ProductEntity> nameProductMap
    ) {
        String bomStringFromEntity = product.getBomString();
        if (bomStringFromEntity == null || bomStringFromEntity.isEmpty()) {
            return;
        }

        if (MULTIPLE_APPROCH_BOM_PATTERN.matcher(bomStringFromEntity)
                .find()) {
            String replacedOr = bomStringFromEntity.replaceAll(MULTIPLE_APPROCH_BOM_PATTERN.pattern(), ",");
            String[] split = replacedOr.split(",");

            List<String> materialStringSplitedList = Arrays.stream(split)
                    .filter(mayBomString -> !mayBomString.isEmpty())
                    .toList();

            for (int i = 0; i < materialStringSplitedList.size(); i++) {
                String bomString = materialStringSplitedList.get(i);
                Matcher matcher = AMOUNT_MATERIAL_PATTERN.matcher(bomString);
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
            Matcher matcher = AMOUNT_MATERIAL_PATTERN.matcher(bomStringFromEntity);
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
        String rawMaterial = matcher.group(2)
                .trim()
                .toLowerCase();
        String materialProductName1 = English.plural(rawMaterial, 1);
        String materialProductName2 = jaroWinklerSimilarityLookup(
                rawMaterial,
                nameProductMap.keySet()
        );

        ProductEntity goodsMaterialDto1 = nameProductMap.get(materialProductName1);
        ProductEntity goodsMaterialDto2 = nameProductMap.get(materialProductName2);
        if (materialProductName1.equalsIgnoreCase(materialProductName2)) {
            if (Objects.nonNull(goodsMaterialDto1)) {
                assert goodsMaterialDto1 == goodsMaterialDto2;
                return goodsMaterialDto1;
            }
        } else if (Objects.nonNull(goodsMaterialDto2)) {
            return goodsMaterialDto2;
        }
        throw new IllegalStateException("couldn't find rawMaterial %s".formatted(rawMaterial));
    }

    private String jaroWinklerSimilarityLookup(String example, Set<String> candidates) {
        return candidates.stream()
                .map(productNameAsMaterial -> new Pair<>(
                        productNameAsMaterial,
                        JARO_WINKLER_SIMILARITY.apply(example, productNameAsMaterial)
                ))
                .filter(pair -> pair.value0() != null && pair.value1() != null)
                .max(Comparator.comparingDouble(Pair::value1))
                .map(Pair::value0)
                .get();
    }

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
            ContextProductHierarchyStructure contextProductHierarchyStructure,
            Duration duration
    ) {
        ProductManufactureInfoEntity productManufactureInfoEntity = new ProductManufactureInfoEntity();
        if (contextProductHierarchyStructure != null && !contextProductHierarchyStructure.boolAtomicProduct()) {
            Set<ProductMaterialsRelation> productMaterialsRelations = buildProductMaterialRelationSet(
                    productManufactureInfoEntity,
                    contextProductHierarchyStructure
            );
            productMaterialsRelations.forEach(productManufactureInfoEntity::attacheProductMaterialsRelation);
        }

        productManufactureInfoEntity.setProducingDuration(Objects.requireNonNullElse(duration, Duration.ZERO));
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
            return graph.vertexSet()
                    .stream()
                    .map(this::resultByGroupInProduct)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toUnmodifiableSet());
        }

        public List<ContextProductHierarchyStructure> resultByGroupInProduct(ProductEntity productEntity) {
            Set<ContextProductHierarchyGraphEdge> productHierarchyGraphEdges = graph.incomingEdgesOf(productEntity);
            return productHierarchyGraphEdges.stream()
                    .collect(
                            Collectors.collectingAndThen(
                                    Collectors.groupingBy(ContextProductHierarchyGraphEdge::getGroupId),
                                    integerListMap -> {
                                        List<ContextProductHierarchyStructure> result = new ArrayList<>();
                                        for (Map.Entry<Integer, List<ContextProductHierarchyGraphEdge>> entry : integerListMap.entrySet()) {
                                            Integer groupId = entry.getKey();
                                            List<ContextProductHierarchyGraphEdge> materialEdges = entry.getValue();
                                            Map<ProductEntity.ProductId, Integer> materialIdToAmountMap
                                                    = materialEdges.stream()
                                                    .collect(
                                                            Collectors.toMap(
                                                                    edge -> ProductEntity.ProductId.of(edge.getSource()
                                                                            .getId()),
                                                                    ContextProductHierarchyGraphEdge::getAmount
                                                            )
                                                    );
                                            ContextProductHierarchyStructure hierarchyStructure = ContextProductHierarchyStructure.builder()
                                                    .id(groupId)
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
            return this.getSource()
                    .getProductId();
        }

        @Override
        public ProductEntity getSource() {
            return (ProductEntity) super.getSource();
        }

        public ProductEntity.ProductId getTargetId() {
            return this.getTarget()
                    .getProductId();
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
