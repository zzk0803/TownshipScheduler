package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.persistence.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.WarehouseEntityRepository;
import zzk.townshipscheduler.backend.service.PlayerService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@SpringComponent
@RouteScope
@RouteScopeOwner(PlayerView.class)
@RequiredArgsConstructor
@Getter
@Setter
public class PlayerViewPresenter {

    private final TownshipAuthenticationContext townshipAuthenticationContext;

    private final PlayerService playerService;

    private final ProductEntityRepository productEntityRepository;

    private final WarehouseEntityRepository warehouseEntityRepository;

    private PlayerView playerView;

    public boolean validate() {
        return townshipAuthenticationContext != null && townshipAuthenticationContext.getPlayerEntity().isPresent();
    }

    public Set<ProductEntity> fetchProducts() {
        return productEntityRepository.findBy(
                ProductEntity.class,
                Sort.by(Sort.Order.asc("level"))
        );
    }

    public PlayerEntity playerFactoryToCorrespondedLevelInBatch(PlayerEntity playerEntity) {
        return playerService.playerFactoryToCorrespondedLevelInBatch(playerEntity);
    }

    public PlayerEntity playerFactoryToCorrespondedLevelInBatch() {
        if (!validate()) {
            throw new IllegalStateException();
        }
        return playerService.playerFactoryToCorrespondedLevelInBatch(getPlayer());
    }

    public List<FieldFactoryInfoEntity> findAvailableFieldFactoryInfoByPlayer() {
        return playerService.findAvailableFieldFactoryInfoByPlayer(getPlayer());
    }

    public PlayerEntity getPlayer() {
        return townshipAuthenticationContext.getPlayerEntity().get();
    }

    public List<FieldFactoryInfoEntity> findAvailableFieldFactoryInfoByPlayer(int playerLevel, Set<FieldFactoryInfoEntity> allFieldFactoryInfo, List<FieldFactoryEntity> playersFieldFactory) {
        return playerService.findAvailableFieldFactoryInfoByPlayer(playerLevel, allFieldFactoryInfo, playersFieldFactory);
    }

    public List<FieldFactoryInfoEntity> findAvailableFieldFactoryInfo() {
        return playerService.findAvailableFieldFactoryInfo();
    }

    public FieldFactoryEntity saveFieldFactory(FieldFactoryEntity fieldFactoryEntity) {
        return playerService.saveFieldFactory(fieldFactoryEntity, getPlayer());
    }

    public List<FieldFactoryEntity> findFieldFactoryEntityByPlayer() {
        return playerService.findFieldFactoryEntityByPlayer(getPlayer());
    }

    public void updateWarehouseStock(
            Map<ProductEntity, Integer> productEntityIntegerMap
    ) {
        WarehouseEntity warehouseEntity = warehouseEntityRepository.findWarehouseEntityByPlayerEntity(getPlayer());
        playerService.updateWarehouseStock(warehouseEntity, productEntityIntegerMap);
    }

    public WarehouseEntity updateWarehouseStock(ProductEntity productEntity, Integer amount) {
        WarehouseEntity warehouseEntity = warehouseEntityRepository.findWarehouseEntityByPlayerEntity(getPlayer());
        return playerService.updateWarehouseStock(
                warehouseEntity,
                productEntity,
                amount
        );
    }

    public WarehouseEntity findWarehouseEntityByPlayerEntity() {
        return warehouseEntityRepository.findWarehouseEntityByPlayerEntity(getPlayer());
    }

    public Supplier<Collection<FieldFactoryInfoEntity>> getCollectionSupplier() {
        return playerService.calcFieldFactoryInfoCollectionSupplier();
    }

    public Supplier<Collection<FieldFactoryInfoEntity>> getCollectionSupplier(PlayerEntity player) {
        return playerService.calcFieldFactoryInfoCollectionSupplier(player);
    }

    public PlayerEntity emergeAndUpdate(PlayerEntity player) {
        return playerService.emergeAndUpdate(player);
    }

}
