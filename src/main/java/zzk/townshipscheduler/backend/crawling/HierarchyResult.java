package zzk.townshipscheduler.backend.crawling;

import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;

import java.util.List;

public record HierarchyResult(
        List<ProductEntity> productEntities,
        List<FieldFactoryInfoEntity> fieldFactoryInfoEntities,
        List<ProductMaterialsRelation> productMaterialsRelations
) {

}
