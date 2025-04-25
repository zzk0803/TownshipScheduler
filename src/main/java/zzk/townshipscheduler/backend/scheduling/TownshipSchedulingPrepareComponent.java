package zzk.townshipscheduler.backend.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.dao.FieldFactoryInfoEntityRepository;
import zzk.townshipscheduler.backend.dao.PlayerEntityRepository;
import zzk.townshipscheduler.backend.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TownshipSchedulingPrepareComponent {

    private final PlayerEntityRepository playerEntityRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    @Transactional(readOnly = true)
    public TownshipSchedulingRequest buildTownshipSchedulingRequest(TownshipAuthenticationContext townshipAuthenticationContext) {

        final PlayerEntity playerEntity = Objects.requireNonNull(townshipAuthenticationContext)
                .getPlayerEntity()
                .orElseThrow();

        final Optional<PlayerEntity> optionalPlayerForScheduling
                = playerEntityRepository.queryForPrepareScheduling(playerEntity.getId());
//                = playerEntityRepository.findPlayerById(playerEntity.getId());

        final Set<FieldFactoryInfoEntity> factoryInfos
                = fieldFactoryInfoEntityRepository.queryForPrepareScheduling(playerEntity.getLevel());
//                = fieldFactoryInfoEntityRepository.findBy(FieldFactoryInfoEntity.class, Sort.by("id"));

        final Set<ProductEntity> products
                = productEntityRepository.queryForPrepareScheduling(playerEntity.getLevel());
//                = productEntityRepository.findBy(ProductEntity.class, Sort.by("id"));

        return optionalPlayerForScheduling
                .map(playerEntityProjection -> TownshipSchedulingRequest.builder()
                        .productEntities(products)
                        .fieldFactoryInfoEntities(factoryInfos)
                        .playerEntityOrderEntities(playerEntityProjection.getOrderEntities())
                        .playerEntityFieldFactoryEntities(playerEntityProjection.getFieldFactoryEntities())
                        .playerEntityWarehouseEntity(playerEntityProjection.getWarehouseEntity())
                        .build()
                )
                .orElseThrow();

    }

}
