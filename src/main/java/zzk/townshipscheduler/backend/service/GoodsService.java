package zzk.townshipscheduler.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.backend.persistence.GoodsRepository;
import zzk.townshipscheduler.port.GoodId;
import zzk.townshipscheduler.port.GoodsDtoForCalcHierarchies;
import zzk.townshipscheduler.port.GoodsHierarchy;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsService {

    private final GoodsRepository goodsRepository;

    private transient Collection<GoodsHierarchy> cachedGoodHierarchies;

    //<editor-fold desc="delegate repository methods">
    public Set<String> queryCategories() {
        return goodsRepository.queryCategories();
    }

    public Optional<Goods> findByName(String name) {
        return goodsRepository.findByName(name);
    }

    public <T> T findById(Class<T> projectionClass, Long id) {
        return goodsRepository.findById(projectionClass, id);
    }

    public <T> List<T> findBy(Class<T> projectionClass) {
        return goodsRepository.findBy(projectionClass);
    }

    public <T> List<T> findBy(Class<T> projectionClass, Sort sort) {
        return goodsRepository.findBy(projectionClass, sort);
    }
    //</editor-fold>

    public Duration calcGoodsDuration(Goods goods) {
        String durationString = goods.getDurationString();
        if (Objects.isNull(durationString)) {
            return Duration.ZERO;
        }

        String replacedOr = durationString.replaceAll("\\sor\\s", ",");
        String replacedSpaceChar = replacedOr.replaceAll("\\s", "");

        Duration goodProductDuration;
        String[] split = replacedSpaceChar.split(",");
        if (split.length > 1) {
            goodProductDuration = Arrays.stream(split).map(ds -> Duration.parse("PT" + ds.toUpperCase())).min(Duration::compareTo).get();
        } else {
            goodProductDuration = Duration.parse("PT" + durationString.toUpperCase());
        }

        return goodProductDuration;
    }

    public GoodsHierarchy calcGoodsHierarchies(Goods goods) {
        if (Objects.isNull(cachedGoodHierarchies) || cachedGoodHierarchies.isEmpty()) {
            calcGoodsHierarchies();
        }

        Collection<GoodsHierarchy> goodsHierarchies = cachedGoodHierarchies;
        return goodsHierarchies.stream()
                .filter(goodsHierarchy -> goodsHierarchy.getGoodId().equals(goods.getId()))
                .findFirst()
                .orElseThrow();
    }

    public Collection<GoodsHierarchy> calcGoodsHierarchies() {
        GoodsHierarchyContext goodsHierarchyContext = new GoodsHierarchyContext();

        List<GoodsDtoForCalcHierarchies> goodsDtoList = goodsRepository.findBy(
                GoodsDtoForCalcHierarchies.class,
                Sort.by("id")
        );
        if (goodsDtoList.isEmpty()) {
            throw new IllegalStateException("it's wired no goods here");
        }

        long systemCurrentTimeMillis = System.currentTimeMillis();
        log.info("calcGoodsHierarchies start");
        for (GoodsDtoForCalcHierarchies currentGoods : goodsDtoList) {
            checkMaterialOfGoodsIntoHierarchy(goodsHierarchyContext, currentGoods, goodsDtoList);
        }
        for (GoodsDtoForCalcHierarchies currentGoods : goodsDtoList) {
            checkProductOfGoodsIntoHierarchy(goodsHierarchyContext, currentGoods, goodsDtoList);
        }
        log.info(
                "calcGoodsHierarchies end...result in {} items,{} passed",
                goodsHierarchyContext.getIdHierarchyMap().size(),
                System.currentTimeMillis() - systemCurrentTimeMillis
        );

        return this.cachedGoodHierarchies = goodsHierarchyContext.getIdHierarchyMap().values();
    }

    private void checkMaterialOfGoodsIntoHierarchy(
            GoodsHierarchyContext goodsHierarchyContext,
            GoodsDtoForCalcHierarchies src,
            List<GoodsDtoForCalcHierarchies> keyList
    ) {
        String bomString = src.getBomString();
        if (bomString == null || bomString.isEmpty()) {
            return;
        }

        Pattern pattern = Pattern.compile("(\\d+)\\s+(\\S+)");
        Matcher matcher = pattern.matcher(bomString);

        while (matcher.find()) {
            try {
                int quantity = Integer.parseInt(matcher.group(1));
                String goodsName = matcher.group(2).trim();

                GoodsDtoForCalcHierarchies goodsMaterialDto = keyList.stream().filter(dto -> dto != src && dto.getName().equalsIgnoreCase(
                        goodsName)).findFirst().orElseThrow();

                goodsHierarchyContext.getIdHierarchyMap()
                        .computeIfAbsent(
                                src.getId(),
                                key -> GoodsHierarchy.builder()
                                        .goodId(GoodId.of(src.getId()))
                                        .composite(new ArrayList<>())
                                        .materials(new HashMap<>())
                                        .build()
                        )
                        .getMaterials().putIfAbsent(GoodId.of(goodsMaterialDto.getId()), quantity);

            } catch (RuntimeException e) {
                //ignore and do nothing
            }
        }
    }

    private void checkProductOfGoodsIntoHierarchy(
            GoodsHierarchyContext goodsHierarchyContext,
            GoodsDtoForCalcHierarchies gdch,
            List<GoodsDtoForCalcHierarchies> keyList
    ) {
        Map<Long, GoodsHierarchy> goodsHierarchyMap = goodsHierarchyContext.getIdHierarchyMap();

        Long goodId = gdch.getId();
        String name = gdch.getName();

        ArrayList<GoodId> productIdList;
        if (goodsHierarchyMap.containsKey(goodId)) {
            productIdList = goodsHierarchyMap.entrySet().stream()
                    .filter(entry -> entry.getValue().getMaterials().containsKey(goodId))
                    .map(idHierarchyEntry -> GoodId.of(idHierarchyEntry.getKey()))
                    .collect(Collectors.toCollection(ArrayList::new));
            goodsHierarchyMap.get(goodId).getComposite().addAll(productIdList);
        }
    }

    private class GoodsHierarchyContext {

        private final Map<Long, GoodsHierarchy> idHierarchyMap;

        public GoodsHierarchyContext() {
            idHierarchyMap = new HashMap<>();
        }

        public Map<Long, GoodsHierarchy> getIdHierarchyMap() {
            return idHierarchyMap;
        }


    }

}
