package zzk.townshipscheduler.ui.views.product;

import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.parameters.P;
import zzk.townshipscheduler.backend.persistence.ProductAmountBill;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;
import zzk.townshipscheduler.backend.persistence.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.dao.ProductManufactureInfoEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.ProductMaterialsRelationRepository;

import java.util.List;
import java.util.Set;

@SpringComponent
@RequiredArgsConstructor
@Getter
@Setter
public class ProductViewPresenter {

    private final ProductEntityRepository productEntityRepository;

    private final ProductManufactureInfoEntityRepository productManufactureInfoEntityRepository;

    private final ProductMaterialsRelationRepository productMaterialsRelationRepository;

    private ProductView productView;

    public Set<ProductEntity> fetchProducts() {
        return productEntityRepository.findBy(
                ProductEntity.class,
                Sort.by(Sort.Order.asc("level"))
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
