package zzk.townshipscheduler.backend.persistence;

import java.util.Set;

public class TownshipArrangementEntity {

    private String uuid;

    private TownshipProblemEntity townshipProblemEntity;

    private OrderEntity order;

    private ProductEntity targetProduct;

    private ProductEntity currentProduct;

    private Set<TownshipArrangementEntity> prerequisiteProducingArrangementEntitySet;

    private ProductManufactureInfoEntity executeMode;


}
