package zzk.townshipscheduler.ui.views.product;

import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.parameters.P;
import org.springframework.transaction.annotation.Transactional;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.persistence.dao.*;
import zzk.townshipscheduler.ui.components.ProductImagesBytesComponent;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@SpringComponent
@RequiredArgsConstructor
@Getter
@Setter
public class ProductViewPresenter {

    private final ProductEntityRepository productEntityRepository;

    private final ProductManufactureInfoEntityRepository productManufactureInfoEntityRepository;

    private final ProductMaterialsRelationRepository productMaterialsRelationRepository;

    private final FieldFactoryInfoEntityRepository fieldFactoryInfoEntityRepository;

    public byte[] getProductImage(ProductEntity productEntity) {
        return productImagesBytesComponent.getProductImage(productEntity);
    }

    private final ProductImagesBytesComponent productImagesBytesComponent;

    private ProductView productView;

    public Set<FieldFactoryInfoEntity> getFieldFactoryInfoCollection() {
        return this.fieldFactoryInfoEntityRepository.queryForFactoryProductSelection(
                Sort.by(
                        Sort.Direction.ASC,
                        "level"
                )
        );
    }

    public void getMaterials(ProductEntity productEntity) {
        Set<ProductManufactureInfoEntity> manufactureInfoEntities = productEntity.getManufactureInfoEntities();
        List<manufactureInfoMaterialsPair> pairs = manufactureInfoEntities.stream()
                .map(manufactureInfoEntity -> new manufactureInfoMaterialsPair(manufactureInfoEntity, manufactureInfoEntity.toProductAmountBill()))
                .toList();
    }

    public void getComposite(ProductEntity productEntity) {

    }

    public record manufactureInfoMaterialsPair(
            ProductManufactureInfoEntity manufactureInfo,
            ProductAmountBill productAmountBill
            ) {

    }

}
