package zzk.townshipscheduler.backend.crawling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.dao.FieldFactoryInfoEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.ProductEntityRepository;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class TownshipDataHardcodeHotfixProcessor {

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final TransactionTemplate transactionTemplate;

    public void process() {
        log.info("going to hardcode fix factoryinfo");
        Map<String, String[]> farmingProductMap = Map.of(
                "Cowshed", new String[]{"Milk"},
                "Chicken Coop", new String[]{"Egg"},
                "Sheep Farm", new String[]{"Wool"},
                "Apiary", new String[]{"Honeycombs"},
                "Pig Farm", new String[]{"Bacon"},
                "Duck Feeder", new String[]{"Down Feather", "Colorful Feather"},
                "Otter Pond", new String[]{"Seaweed", "Scallop", "Pearls"},
                "Mushroom Farm", new String[]{"Mushroom"}
        );
        Map<String, Integer> farmingLevelMap = Map.of(
                "Cowshed", 1,
                "Chicken Coop", 5,
                "Sheep Farm", 10,
                "Apiary", 35,
                "Pig Farm", 42,
                "Duck Feeder", 48,
                "Otter Pond", 58,
                "Mushroom Farm", 63
        );

        String[] threeInstanceFarmingPart = {"Cowshed", "Chicken Coop"};
        Arrays.stream(threeInstanceFarmingPart).map(categoryString -> {
            FieldFactoryInfoEntity fieldFactoryInfo = new FieldFactoryInfoEntity();
            fieldFactoryInfo.setCategory(categoryString);
            fieldFactoryInfo.setLevel(farmingLevelMap.get(categoryString));
            fieldFactoryInfo.setProducingType(FieldFactoryInfoEntity.ProducingType.SLOT);
            fieldFactoryInfo.setDefaultInstanceAmount(1);
            fieldFactoryInfo.setDefaultProducingCapacity(3);
            fieldFactoryInfo.setDefaultReapWindowCapacity(3);
            fieldFactoryInfo.setMaxInstanceAmount(3);
            fieldFactoryInfo.setMaxProducingCapacity(6);
            fieldFactoryInfo.setMaxReapWindowCapacity(6);
            return fieldFactoryInfo;
        }).forEach(fieldFactoryInfoEntity -> {
            String[] strings = farmingProductMap.get(fieldFactoryInfoEntity.getCategory());
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                FieldFactoryInfoEntity savedFieldFactoryInfo = fieldFactoryInfoEntityRepository.save(
                        fieldFactoryInfoEntity);
                Arrays.stream(strings).map(productEntityRepository::findByName)
                        .forEach(productEntity -> productEntity.ifPresent(savedFieldFactoryInfo::attacheProductEntity));
            });
        });

        String[] twoInstanceFarmingPart = {"Sheep Farm", "Apiary", "Pig Farm"};
        Arrays.stream(twoInstanceFarmingPart).map(categoryString -> {
            FieldFactoryInfoEntity fieldFactoryInfo = new FieldFactoryInfoEntity();
            fieldFactoryInfo.setCategory(categoryString);
            fieldFactoryInfo.setLevel(farmingLevelMap.get(categoryString));
            fieldFactoryInfo.setProducingType(FieldFactoryInfoEntity.ProducingType.SLOT);
            fieldFactoryInfo.setDefaultInstanceAmount(1);
            fieldFactoryInfo.setDefaultProducingCapacity(3);
            fieldFactoryInfo.setDefaultReapWindowCapacity(3);
            fieldFactoryInfo.setMaxInstanceAmount(3);
            fieldFactoryInfo.setMaxProducingCapacity(6);
            fieldFactoryInfo.setMaxReapWindowCapacity(6);
            return fieldFactoryInfo;
        }).forEach(fieldFactoryInfoEntity -> {
            String[] strings = farmingProductMap.get(fieldFactoryInfoEntity.getCategory());
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                FieldFactoryInfoEntity savedFieldFactoryInfo = fieldFactoryInfoEntityRepository.save(
                        fieldFactoryInfoEntity);
                Arrays.stream(strings).map(productEntityRepository::findByName)
                        .forEach(productEntity -> productEntity.ifPresent(savedFieldFactoryInfo::attacheProductEntity));
            });
        });

        FieldFactoryInfoEntity duckFeeder = new FieldFactoryInfoEntity();
        duckFeeder.setCategory("Duck Feeder");
        duckFeeder.setLevel(48);
        duckFeeder.setProducingType(FieldFactoryInfoEntity.ProducingType.SLOT);
        duckFeeder.setDefaultInstanceAmount(1);
        duckFeeder.setDefaultProducingCapacity(3);
        duckFeeder.setDefaultReapWindowCapacity(3);
        duckFeeder.setMaxInstanceAmount(2);
        duckFeeder.setMaxProducingCapacity(3);
        duckFeeder.setMaxReapWindowCapacity(3);
        String[] duckFeederProducts = farmingProductMap.get(duckFeeder.getCategory());
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            FieldFactoryInfoEntity savedFieldFactoryInfo = fieldFactoryInfoEntityRepository.save(duckFeeder);
            Arrays.stream(duckFeederProducts).
                    map(productEntityRepository::findByName)
                    .forEach(productEntity -> productEntity.ifPresent(savedFieldFactoryInfo::attacheProductEntity));
        });

        FieldFactoryInfoEntity otterPond = new FieldFactoryInfoEntity();
        otterPond.setCategory("Otter Pond");
        otterPond.setLevel(58);
        otterPond.setProducingType(FieldFactoryInfoEntity.ProducingType.SLOT);
        otterPond.setDefaultInstanceAmount(1);
        otterPond.setDefaultProducingCapacity(3);
        otterPond.setDefaultReapWindowCapacity(3);
        otterPond.setMaxInstanceAmount(1);
        otterPond.setMaxProducingCapacity(3);
        otterPond.setMaxReapWindowCapacity(3);
        String[] otterPondProducts = farmingProductMap.get(otterPond.getCategory());
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            FieldFactoryInfoEntity savedFieldFactoryInfo = fieldFactoryInfoEntityRepository.save(otterPond);
            Arrays.stream(otterPondProducts).map(productEntityRepository::findByName)
                    .forEach(productEntity -> productEntity.ifPresent(savedFieldFactoryInfo::attacheProductEntity));
        });

        FieldFactoryInfoEntity mushroomFarm = new FieldFactoryInfoEntity();
        mushroomFarm.setCategory("Mushroom Farm");
        mushroomFarm.setLevel(63);
        mushroomFarm.setProducingType(FieldFactoryInfoEntity.ProducingType.SLOT);
        mushroomFarm.setDefaultInstanceAmount(1);
        mushroomFarm.setDefaultProducingCapacity(3);
        mushroomFarm.setDefaultReapWindowCapacity(3);
        mushroomFarm.setMaxInstanceAmount(1);
        mushroomFarm.setMaxProducingCapacity(6);
        mushroomFarm.setMaxReapWindowCapacity(6);
        String[] mushroomFarmProducts = farmingProductMap.get(mushroomFarm.getCategory());
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            FieldFactoryInfoEntity savedFieldFactoryInfo = fieldFactoryInfoEntityRepository.save(mushroomFarm);
            Arrays.stream(mushroomFarmProducts).map(productEntityRepository::findByName)
                    .forEach(productEntity -> productEntity.ifPresent(savedFieldFactoryInfo::attacheProductEntity));
        });

        transactionTemplate.executeWithoutResult(_ -> {
            Optional<FieldFactoryInfoEntity> farmBuildings
                    = fieldFactoryInfoEntityRepository.findByCategory("Farm Buildings");
            farmBuildings.ifPresent(fieldFactoryInfoEntityRepository::delete);
        });

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<FieldFactoryInfoEntity> feedMillOptional
                    = fieldFactoryInfoEntityRepository.findByCategory("Feed Mill");
            FieldFactoryInfoEntity feedMill = feedMillOptional.orElseThrow();
            feedMill.setLevel(3);
            feedMill.setDefaultInstanceAmount(1);
            feedMill.setDefaultProducingCapacity(3);
            feedMill.setDefaultReapWindowCapacity(6);
            feedMill.setMaxInstanceAmount(3);
            feedMill.setMaxProducingCapacity(7);
            feedMill.setMaxReapWindowCapacity(8);
            fieldFactoryInfoEntityRepository.save(feedMill);
        });

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<FieldFactoryInfoEntity> fieldOptional
                    = fieldFactoryInfoEntityRepository.findByCategory("Crops");
            FieldFactoryInfoEntity field = fieldOptional.orElseThrow();
            field.setCategory("Field");
            field.setLevel(1);
            field.setProducingType(FieldFactoryInfoEntity.ProducingType.SLOT);
            field.setDefaultInstanceAmount(6);
            field.setDefaultProducingCapacity(1);
            field.setDefaultReapWindowCapacity(1);
            field.setMaxInstanceAmount(146);
            field.setMaxProducingCapacity(1);
            field.setMaxReapWindowCapacity(1);
            fieldFactoryInfoEntityRepository.save(field);
        });

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<FieldFactoryInfoEntity> islandShipOptional
                    = fieldFactoryInfoEntityRepository.findByCategory("Islands and Ships");
            FieldFactoryInfoEntity islandShip = islandShipOptional.orElseThrow();
            islandShip.setCategory("IslandsShip");
            islandShip.setLevel(29);
            islandShip.setProducingType(FieldFactoryInfoEntity.ProducingType.SLOT);
            islandShip.setDefaultInstanceAmount(1);
            islandShip.setDefaultProducingCapacity(1);
            islandShip.setDefaultReapWindowCapacity(3);
            islandShip.setMaxInstanceAmount(4);
            islandShip.setMaxProducingCapacity(1);
            islandShip.setMaxReapWindowCapacity(3);
            fieldFactoryInfoEntityRepository.save(islandShip);
        });

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<FieldFactoryInfoEntity> foundryOptional = fieldFactoryInfoEntityRepository.findByCategory("Foundry");
            FieldFactoryInfoEntity foundry = foundryOptional.orElseThrow();
            foundry.setLevel(21);
            foundry.setProducingType(FieldFactoryInfoEntity.ProducingType.SLOT);
            foundry.setDefaultInstanceAmount(1);
            foundry.setDefaultProducingCapacity(1);
            foundry.setDefaultReapWindowCapacity(1);
            foundry.setMaxInstanceAmount(3);
            foundry.setMaxProducingCapacity(1);
            foundry.setMaxReapWindowCapacity(1);
            fieldFactoryInfoEntityRepository.save(foundry);
        });

        log.info("going to hardcode fix factoryinfo method over");
    }

}
