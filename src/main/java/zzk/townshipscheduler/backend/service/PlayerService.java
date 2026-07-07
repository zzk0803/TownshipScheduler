package zzk.townshipscheduler.backend.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.persistence.dao.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final TownshipAuthenticationContext townshipAuthenticationContext;

    private final AppUserEntityRepository appUserEntityRepository;

    private final PlayerEntityRepository playerEntityRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final FieldFactoryEntityRepository fieldFactoryEntityRepository;

    private final OrderEntityRepository orderEntityRepository;

    private final WarehouseEntityRepository warehouseEntityRepository;

    private final TransactionTemplate transactionTemplate;

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
    public List<FieldFactoryInfoEntity> findAvailableFieldFactoryInfo() {
        return new ArrayList<>(fieldFactoryInfoEntityRepository.findBy(FieldFactoryInfoEntity.class));
    }

    @Transactional(readOnly = true)
    public List<FieldFactoryInfoEntity> findAvailableFieldFactoryInfoByPlayer(PlayerEntity player) {
        Set<FieldFactoryInfoEntity> allFieldFactoryInfo
                = fieldFactoryInfoEntityRepository.findBy(FieldFactoryInfoEntity.class);
        List<FieldFactoryEntity> playersFieldFactory
                = fieldFactoryEntityRepository.findFieldFactoryEntityByPlayerEntity(player);
        return findAvailableFieldFactoryInfoByPlayer(player.getLevel(), allFieldFactoryInfo, playersFieldFactory);
    }

    public List<FieldFactoryInfoEntity> findAvailableFieldFactoryInfoByPlayer(int playerLevel, Set<FieldFactoryInfoEntity> allFieldFactoryInfo, List<FieldFactoryEntity> playersFieldFactory) {
        Map<FieldFactoryInfoEntity, Long> playerInfoHavingMap
                = playersFieldFactory.stream()
                .collect(Collectors.groupingBy(
                                FieldFactoryEntity::getFieldFactoryInfoEntity,
                                Collectors.counting()
                        )
                );

        return allFieldFactoryInfo.stream()
                .filter(fieldFactoryInfoEntity -> {
                    boolean levelFilterBool = fieldFactoryInfoEntity.getLevel() <= playerLevel;
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
        playerEntity.addFieldFactory(fieldFactoryEntity);
        PlayerEntity mergedPlayer = playerEntityRepository.save(playerEntity);
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

    public List<FieldFactoryEntity> playerLevelUpdateAndSetupRelatedFactories(PlayerEntity playerEntity) {

        return transactionTemplate.execute(status -> {
            List<FieldFactoryInfoEntity> fieldFactoryInfoEntitiesByLevelBetween
                    = fieldFactoryInfoEntityRepository.findFieldFactoryInfoEntitiesByLevelBetween(
                    playerEntity.getLevel() - 1,
                    playerEntity.getLevel()
            );
            return fieldFactoryInfoEntitiesByLevelBetween.stream()
                    .map(fieldFactoryInfoEntity -> {
                        FieldFactoryEntity fieldFactoryEntity = fieldFactoryInfoEntity.toFieldFactoryEntity(
                                () -> playerEntity);
                        fieldFactoryEntity.setProducingLength(fieldFactoryInfoEntity.getDefaultProducingCapacity());
                        fieldFactoryEntity.setReapWindowSize(fieldFactoryInfoEntity.getDefaultReapWindowCapacity());
                        return fieldFactoryEntityRepository.save(fieldFactoryEntity);
                    })
                    .toList();
        });
    }

    public PlayerEntity emergeAndUpdate(PlayerEntity player) {
        return transactionTemplate.execute(_ -> playerEntityRepository.save(player));
    }

    @Transactional
    public PlayerEntity clearPlayerFieldFactory(PlayerEntity playerEntity) {
        playerEntity.removeAllFieldFactory();
        return playerEntityRepository.saveAndFlush(playerEntity);
    }

    @Transactional
    public PlayerEntity playerFactoryToCorrespondedLevelInBatch(PlayerEntity playerEntity) {
        playerEntity.removeAllFieldFactory();
        PlayerEntity managedPlayer = playerEntityRepository.saveAndFlush(playerEntity);

        FieldFactoryInfoEntity field
                = fieldFactoryInfoEntityRepository.findByCategory(FieldFactoryInfoEntity.FIELD_CATEGORY_CRITERIA)
                .orElseThrow();
        Set<FieldFactoryEntity> factoryEntities = IntStream.range(0, managedPlayer.getFieldAmount())
                .mapToObj(i -> field.toFieldFactoryEntity(() -> managedPlayer))
                .collect(Collectors.toSet());
        managedPlayer.addAllFieldFactory(factoryEntities);
        playerEntityRepository.save(managedPlayer);

        List<FieldFactoryInfoEntity> availableFieldFactoryInfoAsList
                = fieldFactoryInfoEntityRepository.findFieldFactoryInfoEntitiesByLevelLessThan(managedPlayer.getLevel());
        availableFieldFactoryInfoAsList.removeIf(fieldFactoryInfoEntity -> fieldFactoryInfoEntity.getCategory().equals("Crops"));
        availableFieldFactoryInfoAsList.removeIf(fieldFactoryInfoEntity -> fieldFactoryInfoEntity.getCategory().equals(FieldFactoryInfoEntity.FIELD_CATEGORY_CRITERIA));

        factoryEntities = availableFieldFactoryInfoAsList.stream()
                .map(
                        fieldFactoryInfoEntity -> IntStream.range(0, fieldFactoryInfoEntity.getMaxInstanceAmount())
                                .mapToObj(_ -> {
                                    FieldFactoryEntity fieldFactoryEntity
                                            = fieldFactoryInfoEntity.toFieldFactoryEntity(() -> managedPlayer);
                                    fieldFactoryEntity.setProducingLength(fieldFactoryInfoEntity.getMaxProducingCapacity());
                                    fieldFactoryEntity.setReapWindowSize(fieldFactoryInfoEntity.getMaxReapWindowCapacity());
                                    return fieldFactoryEntity;
                                })
                                .toList()
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        managedPlayer.addAllFieldFactory(factoryEntities);
        return playerEntityRepository.saveAndFlush(managedPlayer);

    }

    public WarehouseEntity updateWarehouseStock(
            WarehouseEntity warehouseEntity,
            Map<ProductEntity, Integer> productEntityIntegerMap
    ) {
        return transactionTemplate.execute(status -> {
            warehouseEntity.changeProductAmount(productEntityIntegerMap);
            return warehouseEntityRepository.saveAndFlush(warehouseEntity);
        });
    }

    public WarehouseEntity updateWarehouseStock(
            WarehouseEntity playerWarehouse,
            ProductEntity productEntity,
            Integer amount
    ) {
        return transactionTemplate.execute(status -> {
            playerWarehouse.doStockAction(productEntity, WarehouseEntity.WarehouseAction.SAVE, amount);
            return warehouseEntityRepository.saveAndFlush(playerWarehouse);
        });
    }

    public Supplier<Collection<FieldFactoryInfoEntity>> calcFieldFactoryInfoCollectionSupplier() {
        if (getTownshipAuthenticationContext() != null && getTownshipAuthenticationContext().getPlayerEntity().isPresent()) {
            return calcFieldFactoryInfoCollectionSupplier(getTownshipAuthenticationContext().getPlayerEntity().get());
        }
        return () -> this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                Sort.by(
                        Sort.Direction.ASC,
                        "level"
                )
        );
    }

    public Supplier<Collection<FieldFactoryInfoEntity>> calcFieldFactoryInfoCollectionSupplier(PlayerEntity player) {
        return () -> this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                player.getLevel(),
                Sort.by(
                        Sort.Direction.ASC,
                        "level"
                )
        );
    }

    public FieldFactoryEntity updateFieldFactory(FieldFactoryEntity fieldFactoryEntity) {
        return fieldFactoryEntityRepository.saveAndFlush(fieldFactoryEntity);
    }

}
