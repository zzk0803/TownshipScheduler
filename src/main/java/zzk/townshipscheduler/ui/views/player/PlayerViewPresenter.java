package zzk.townshipscheduler.ui.views.player;

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

import java.util.List;
import java.util.Set;

@SpringComponent
@RequiredArgsConstructor
@Getter
@Setter
public class PlayerViewPresenter {

    private final TownshipAuthenticationContext townshipAuthenticationContext;

    private final PlayerService playerService;

    private final ProductEntityRepository productEntityRepository;

    private final WarehouseEntityRepository warehouseEntityRepository;

    private PlayerView playerView;

    public PlayerEntity getPlayer() {
        return townshipAuthenticationContext.getPlayerEntity().get();
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

    public List<FieldFactoryInfoEntity> findAvailableFieldFactoryInfoByPlayer() {
        return playerService.findAvailableFieldFactoryInfoByPlayer(getPlayer());
    }

    public FieldFactoryEntity saveFieldFactory(FieldFactoryEntity fieldFactoryEntity) {
        return playerService.saveFieldFactory(fieldFactoryEntity, getPlayer());
    }

    public List<FieldFactoryEntity> findFieldFactoryEntityByPlayer() {
        return playerService.findFieldFactoryEntityByPlayer(getPlayer());
    }

    public WarehouseEntity updateWarehouseStock(ProductEntity productEntity, Integer amount) {
        return playerService.updateWarehouseStock(
                warehouseEntityRepository.findWarehouseEntityByPlayerEntity(getPlayer()),
                productEntity,
                amount
        );
    }

    public WarehouseEntity findWarehouseEntityByPlayerEntity() {
        return warehouseEntityRepository.findWarehouseEntityByPlayerEntity(getPlayer());
    }

}
