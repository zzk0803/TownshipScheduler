package zzk.project.vaadinproject.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import zzk.project.vaadinproject.backend.persistence.Goods;
import zzk.project.vaadinproject.backend.persistence.GoodsForHierarchyDto;
import zzk.project.vaadinproject.backend.persistence.GoodsHierarchy;
import zzk.project.vaadinproject.backend.persistence.GoodsRepository;

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
        if (durationString == null || durationString.isBlank()) {
            return Duration.ZERO;
        }

        String processing = durationString.replaceAll("\\s", "");
        // 正则表达式匹配小时h, 分钟m, 和秒s
        Pattern pattern = Pattern.compile("(\\d+h)?(\\d+m)?(\\d+s)?");
        Matcher matcher = pattern.matcher(processing);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time format: " + processing);
        }

        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        // 提取匹配到的小时、分钟、秒
        if (matcher.group(1) != null) {
            hours = Integer.parseInt(matcher.group(1).replaceAll("[^0-9]", ""));
        }
        if (matcher.group(2) != null) {
            minutes = Integer.parseInt(matcher.group(2).replaceAll("[^0-9]", ""));
        }
        if (matcher.group(3) != null) {
            seconds = Integer.parseInt(matcher.group(3).replaceAll("[^0-9]", ""));
        }

        return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
    }

    @Deprecated
    public Map<Goods, Integer> queryBom(Goods goods) {
        Map<Goods, Integer> map = new HashMap<>();

        if (goods.getBomString() == null) {
            return map;
        }

        Pattern pattern = Pattern.compile("(\\d+)\\s+(\\S+)");
        Matcher matcher = pattern.matcher(goods.getBomString());

        while (matcher.find()) {
            try {
                int quantity = Integer.parseInt(matcher.group(1));
                String item = matcher.group(2);

                Optional<Goods> goodComponentOptional = goodsRepository.findByName(item);
                Goods goodComponent = goodComponentOptional.orElseThrow(() -> new RuntimeException("no such goods ->" + item));
                map.put(goodComponent, quantity);
            } catch (RuntimeException e) {
                throw new RuntimeException(e);
            }
        }

        return map;
    }

    public Collection<GoodsHierarchy> calcGoodsHierarchies() {
        GoodsHierarchyContext goodsHierarchyContext = new GoodsHierarchyContext();

        List<GoodsForHierarchyDto> goodsDtoList = goodsRepository.findBy(GoodsForHierarchyDto.class, Sort.by("id"));
        if (goodsDtoList.isEmpty()) {
            throw new IllegalStateException("it's wired no goods here");
        }

        long systemCurrentTimeMillis = System.currentTimeMillis();
        log.info("calcGoodsHierarchies start");
        for (GoodsForHierarchyDto currentGoods : goodsDtoList) {
            checkMaterialOfGoodsIntoHierarchy(goodsHierarchyContext, currentGoods, goodsDtoList);
        }
        for (GoodsForHierarchyDto currentGoods : goodsDtoList) {
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
            GoodsHierarchyContext goodsHierarchyContext, GoodsForHierarchyDto src, List<GoodsForHierarchyDto> keyList
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

                GoodsForHierarchyDto goodsMaterialDto = keyList.stream()
                        .filter(dto -> dto != src && dto.getName().equalsIgnoreCase(goodsName))
                        .findFirst().orElseThrow();

                goodsHierarchyContext.getIdGoodsMap()
                        .computeIfAbsent(
                                src.getId(),
                                key -> GoodsHierarchy.builder()
                                        .goodId(src.getId())
                                        .advancedProductList(new ArrayList<>())
                                        .consistMaterialMap(new HashMap<>())
                                        .build()
                        )
                        .getConsistMaterialMap()
                        .putIfAbsent(goodsMaterialDto.getId(), quantity);

            } catch (RuntimeException e) {
                //ignore and do nothing
            }
        }
    }

    private void checkProductOfGoodsIntoHierarchy(
            GoodsHierarchyContext goodsHierarchyContext, GoodsForHierarchyDto src, List<GoodsForHierarchyDto> keyList
    ) {
        Map<Long, GoodsHierarchy> goodsHierarchyMap = goodsHierarchyContext.getIdGoodsMap();

        Long goodId = src.getId();
        String name = src.getName();

        ArrayList<Long> productIdList;
        if (goodsHierarchyMap.containsKey(goodId)) {
            productIdList = goodsHierarchyMap.entrySet().stream()
                    .filter(entry -> entry.getValue().getConsistMaterialMap().containsKey(goodId))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(ArrayList::new));
            goodsHierarchyMap.get(goodId).getAdvancedProductList().addAll(productIdList);
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
