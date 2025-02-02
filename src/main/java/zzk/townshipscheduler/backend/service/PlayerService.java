package zzk.townshipscheduler.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zzk.townshipscheduler.backend.dao.*;
import zzk.townshipscheduler.backend.persistence.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final AppUserEntityRepository appUserEntityRepository;

    private final PlayerEntityRepository playerEntityRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final FieldFactoryEntityRepository fieldFactoryEntityRepository;

    private final WarehouseEntityRepository warehouseEntityRepository;

    @Transactional(readOnly = true)
    public List<PlayerEntity> findAllPlayer() {
        return playerEntityRepository.findBy(PlayerEntity.class);
    }

    @Transactional(readOnly = true)
    public Optional<PlayerEntity> findPlayerEntitiesByAppUser(AccountEntity accountEntity) {
        return playerEntityRepository.findPlayerEntitiesByAccount(accountEntity);
    }

    @Transactional(readOnly = true)
    public List<FieldFactoryEntity> findFieldFactoryEntityByPlayer(PlayerEntity playerEntity) {
        return fieldFactoryEntityRepository.findFieldFactoryEntityByPlayerEntity(playerEntity);
    }

    @Transactional(readOnly = true)
    public List<FieldFactoryInfoEntity> findAvailableFieldFactoryInfoByPlayer(PlayerEntity player) {
        Set<FieldFactoryInfoEntity> allFieldFactoryInfo
                = fieldFactoryInfoEntityRepository.findBy(FieldFactoryInfoEntity.class);
        List<FieldFactoryEntity> playersFieldFactory
                = fieldFactoryEntityRepository.findFieldFactoryEntityByPlayerEntity(player);
        Map<FieldFactoryInfoEntity, Long> playerInfoHavingMap
                = playersFieldFactory.stream()
                .collect(Collectors.groupingBy(
                                FieldFactoryEntity::getFieldFactoryInfoEntity,
                                Collectors.counting()
                        )
                );

        return allFieldFactoryInfo.stream()
                .filter(fieldFactoryInfoEntity -> {
                    boolean levelFilterBool = fieldFactoryInfoEntity.getLevel() <= player.getLevel();
                    boolean amountFilterBool = playerInfoHavingMap.getOrDefault(
                            fieldFactoryInfoEntity,
                            0L
                    ) < fieldFactoryInfoEntity.getMaxInstanceAmount();
                    return levelFilterBool & amountFilterBool;
                })
                .toList();
    }

    @Transactional
    public FieldFactoryEntity saveFieldFactory(FieldFactoryEntity fieldFactoryEntity, PlayerEntity playerEntity) {
        PlayerEntity mergedPlayer = playerEntityRepository.save(playerEntity);
        fieldFactoryEntity.setPlayerEntity(mergedPlayer);
        int alreadyHave = fieldFactoryEntityRepository.countByPlayerEntityAndFieldFactoryInfoEntity(
                mergedPlayer,
                fieldFactoryEntity.getFieldFactoryInfoEntity()
        );
        if (alreadyHave < fieldFactoryEntity.getFieldFactoryInfoEntity().getMaxInstanceAmount()) {
            return fieldFactoryEntityRepository.save(fieldFactoryEntity);
        } else {
            throw new RuntimeException("field factory amount exceed its max instance limit");
        }
    }

    @Transactional(readOnly = true)
    public WarehouseEntity findWarehouseEntityByPlayerEntity(PlayerEntity playerEntity) {
        return warehouseEntityRepository.findWarehouseEntityByPlayerEntity(playerEntity);
    }

    @Transactional
    public PlayerEntity emergeAndUpdate(PlayerEntity player) {
        return playerEntityRepository.save(player);
    }

    @Transactional
    public void playerFactoryToCorrespondedLevelInBatch(PlayerEntity playerEntity) {
        List<FieldFactoryInfoEntity> availableFieldFactoryInfoAsList
                = fieldFactoryInfoEntityRepository.queryFactoryInfoByLevelLessThanOrEqual(
                playerEntity.getLevel()
        );

        PlayerEntity managedPlayer = playerEntityRepository.save(playerEntity);
        FieldFactoryInfoEntity field
                = fieldFactoryInfoEntityRepository.findByCategory(FieldFactoryInfoEntity.FIELD_CRITERIA)
                .orElseThrow();
        IntStream.range(0, managedPlayer.getFieldAmount())
                .mapToObj(i -> field.toFieldFactoryEntity(()->managedPlayer))
                .forEach(fieldFactoryEntityRepository::save);

        availableFieldFactoryInfoAsList.forEach(
                fieldFactoryInfoEntity -> {
                    IntStream.range(0, fieldFactoryInfoEntity.getMaxInstanceAmount())
                            .forEach(_ -> {
                                FieldFactoryEntity fieldFactoryEntity = fieldFactoryInfoEntity.toFieldFactoryEntity(() -> managedPlayer);
                                fieldFactoryEntity.setProducingLength(fieldFactoryInfoEntity.getMaxProducingCapacity());
                                fieldFactoryEntity.setReapWindowSize(fieldFactoryInfoEntity.getMaxReapWindowCapacity());
                                fieldFactoryEntityRepository.save(fieldFactoryEntity);
                            });
                }
        );

    }

    @Transactional
    public WarehouseEntity updateWarehouseStock(
            WarehouseEntity playerWarehouse,
            ProductEntity productEntity,
            Integer amount
    ) {
        WarehouseEntity mergedWarehouse = warehouseEntityRepository.save(playerWarehouse);
        mergedWarehouse.doStockAction(productEntity, WarehouseEntity.WarehouseAction.SAVE, amount);
        return mergedWarehouse;
    }

}
