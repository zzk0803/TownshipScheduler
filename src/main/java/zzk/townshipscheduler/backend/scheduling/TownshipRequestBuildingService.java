package zzk.townshipscheduler.backend.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.dao.FieldFactoryInfoEntityRepository;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.service.PlayerService;
import zzk.townshipscheduler.backend.service.ProductService;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TownshipRequestBuildingService {

    private final PlayerService playerService;

    private final ProductService productService;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final TownshipAuthenticationContext townshipAuthenticationContext;

//    public TownshipSchedulingRequest backendPrepareTownshipScheduling() {
//        AccountEntity accountEntity = Objects.requireNonNull(townshipAuthenticationContext)
//                .getUserDetails();
//
//        PlayerEntity playerEntity = accountEntity.getPlayerEntity();
//
//        Optional<PlayerEntityDto> playerDtoOptional
//                = playerService.findPlayerDtoEntitiesById(playerEntity.getId());
//
//        final Set<FieldFactoryInfoEntityDto> factoryInfos
//                = fieldFactoryInfoEntityRepository.findBy(
//                FieldFactoryInfoEntityDto.class,
//                Sort.by("id")
//        );
//
//        final Set<ProductEntityDtoForScheduling> products
//                = this.productService.findBy(
//                ProductEntityDtoForScheduling.class,
//                Sort.by("id")
//        );
//
//
//        TownshipSchedulingRequest request = playerDtoOptional
//                .map(playerEntityDto -> {
//                    Set<OrderEntityDto> playerEntityOrderEntities = playerEntityDto.orderEntities();
//                    Set<FieldFactoryEntityDto> playerEntityFieldFactoryEntities = playerEntityDto.fieldFactoryEntities();
//                    WarehouseEntityDto playerEntityWarehouseEntity = playerEntityDto.warehouseEntity();
//                    return TownshipSchedulingRequest.builder()
//                            .productEntities(products)
//                            .fieldFactoryInfoEntities(factoryInfos)
//                            .playerEntityOrderEntities(playerEntityOrderEntities)
//                            .playerEntityFieldFactoryEntities(playerEntityFieldFactoryEntities)
//                            .playerEntityWarehouseEntity(playerEntityWarehouseEntity)
//                            .build();
//                })
//                .orElseThrow();
//
//        return request;
//    }

    public TownshipSchedulingRequest backendPrepareTownshipScheduling(TownshipAuthenticationContext townshipAuthenticationContext) {

        AccountEntity accountEntity
                = Objects.requireNonNull(townshipAuthenticationContext)
                .getUserDetails();

        PlayerEntity playerEntity = accountEntity.getPlayerEntity();

        Optional<PlayerEntityProjection> optionalPlayerEntityProjection
                = playerService.findPlayerProjectionById(playerEntity.getId());
//                = playerService.findPlayerDtoEntitiesById(playerEntity.getId());

        final Set<FieldFactoryInfoEntityProjectionForScheduling> factoryInfos
                = fieldFactoryInfoEntityRepository.findBy(
                FieldFactoryInfoEntityProjectionForScheduling.class,
                Sort.by("id")
        );

        final Set<ProductEntityProjectionForScheduling> products
                = this.productService.findBy(
                ProductEntityProjectionForScheduling.class,
                Sort.by("id")
        );


        TownshipSchedulingRequest request = optionalPlayerEntityProjection
                .map(playerEntityProjection -> {
                    return TownshipSchedulingRequest.builder()
                            .productEntities(products)
                            .fieldFactoryInfoEntities(factoryInfos)
                            .playerEntityOrderEntities(playerEntityProjection.getOrderEntities())
                            .playerEntityFieldFactoryEntities(playerEntityProjection.getFieldFactoryEntities())
                            .playerEntityWarehouseEntity(playerEntityProjection.getWarehouseEntity())
                            .build();
                })
                .orElseThrow();

        return request;

    }

}
