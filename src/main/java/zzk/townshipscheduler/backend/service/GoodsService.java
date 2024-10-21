package zzk.townshipscheduler.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.backend.persistence.GoodsDtoForCalcHierarchies;
import zzk.townshipscheduler.backend.persistence.GoodsHierarchy;
import zzk.townshipscheduler.backend.persistence.GoodsRepository;

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

    public Duration queryDuration(Goods goods) {
        String durationString = goods.getDurationString();
        if (Objects.isNull(durationString)) {
            return Duration.ZERO;
        }

        String replacedOr = durationString.replaceAll("\\sor\\s", ",");
        String replacedSpaceChar = replacedOr.replaceAll("\\s", "");

        Duration goodProductDuration;
        String[] split = replacedSpaceChar.split(",");
        if (split.length > 1) {
            goodProductDuration = Arrays.stream(split)
                    .map(ds -> Duration.parse("PT" + ds.toUpperCase()))
                    .min(Duration::compareTo)
                    .get();
        }
        {
            goodProductDuration = Duration.parse("PT" + durationString.toUpperCase());
        }

        return goodProductDuration;
    }

    public Collection<GoodsHierarchy> calcGoodsHierarchies() {
        GoodsHierarchyContext goodsHierarchyContext = new GoodsHierarchyContext();

        List<GoodsDtoForCalcHierarchies> goodsDtoList = goodsRepository.findBy(GoodsDtoForCalcHierarchies.class, Sort.by("id"));
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
                goodsHierarchyContext.getIdGoodsMap().size(),
                System.currentTimeMillis() - systemCurrentTimeMillis
        );

        return goodsHierarchyContext.getIdGoodsMap().values();
    }

    private void checkMaterialOfGoodsIntoHierarchy(
            GoodsHierarchyContext goodsHierarchyContext, GoodsDtoForCalcHierarchies src, List<GoodsDtoForCalcHierarchies> keyList
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

                GoodsDtoForCalcHierarchies goodsMaterialDto = keyList.stream()
                        .filter(dto -> dto != src && dto.getName().equalsIgnoreCase(goodsName))
                        .findFirst().orElseThrow();

                goodsHierarchyContext.getIdGoodsMap()
                        .computeIfAbsent(
                                src.getId(),
                                key -> GoodsHierarchy.builder()
                                        .goodId(src.getId())
                                        .composite(new ArrayList<>())
                                        .materials(new HashMap<>())
                                        .build()
                        )
                        .getMaterials()
                        .putIfAbsent(goodsMaterialDto.getId(), quantity);

            } catch (RuntimeException e) {
                //ignore and do nothing
            }
        }
    }

    private void checkProductOfGoodsIntoHierarchy(
            GoodsHierarchyContext goodsHierarchyContext, GoodsDtoForCalcHierarchies src, List<GoodsDtoForCalcHierarchies> keyList
    ) {
        Map<Long, GoodsHierarchy> goodsHierarchyMap = goodsHierarchyContext.getIdGoodsMap();

        Long goodId = src.getId();
        String name = src.getName();

        ArrayList<Long> productIdList;
        if (goodsHierarchyMap.containsKey(goodId)) {
            productIdList = goodsHierarchyMap.entrySet().stream()
                    .filter(entry -> entry.getValue().getMaterials().containsKey(goodId))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(ArrayList::new));
            goodsHierarchyMap.get(goodId).getComposite().addAll(productIdList);
        } else {
//            productIdList = keyList.stream()
//                    .filter(dto -> dto.getBomString().contains(name))
//                    .map(GoodsForHierarchyDto::getId)
//                    .collect(Collectors.toCollection(ArrayList::new));
//            Objects.requireNonNull(goodsHierarchyMap.put(
//                    goodId,
//                    GoodsHierarchy.builder()
//                            .goodId(goodId)
//                            .name(name)
//                            .advancedProductList(new ArrayList<>())
//                            .consistMaterialMap(new HashMap<>())
//                            .build()
//            )).getAdvancedProductList().addAll(productIdList);
        }
    }

    public GoodsHierarchy calcGoodsHierarchies(Goods goods) {
//        todo
        return null;
    }

    class GoodsHierarchyContext {

        private final Map<Long, GoodsHierarchy> idGoodsMap;

        public GoodsHierarchyContext() {
            idGoodsMap = new HashMap<>();
        }

        public Map<Long, GoodsHierarchy> getIdGoodsMap() {
            return idGoodsMap;
        }


    }

}
