package zzk.townshipscheduler.ui.views.product;

import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntityRepository;
import zzk.townshipscheduler.backend.persistence.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntityRepository;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelationRepository;
import zzk.townshipscheduler.ui.components.ProductImagesBytesComponent;

import java.util.Set;

@SpringComponent
@RequiredArgsConstructor
@Getter
@Setter
public class ProductViewPresenter {

    private final ProductEntityRepository productEntityRepository;

    private final ProductManufactureInfoEntityRepository productManufactureInfoEntityRepository;

    private final ProductMaterialsRelationRepository productMaterialsRelationRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    private final ProductImagesBytesComponent productImagesBytesComponent;

    private ProductView productView;

    @Transactional(readOnly = true)
    public Set<FieldFactoryInfoEntity> getFieldFactoryInfoCollection() {
        return this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                Sort.by(
                        Sort.Direction.ASC,
                        "level"
                )
        );
    }

    public void getMaterials(ProductEntity productEntity) {
        Set<ProductManufactureInfoEntity> materialsProductsManufactures = productEntity.getManufactureInfoEntities();
    }

    public void getComposite(ProductEntity productEntity) {
        Set<ProductManufactureInfoEntity> compositeProductsManufactures = this.productManufactureInfoEntityRepository.queryProductManufactureInfoByMaterial(productEntity);
    }

}
