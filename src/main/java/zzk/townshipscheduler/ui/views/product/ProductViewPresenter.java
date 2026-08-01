package zzk.townshipscheduler.ui.views.product;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.ui.components.ProductImages;
import zzk.townshipscheduler.ui.components.ProductImagesBytesComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@SpringComponent
@RequiredArgsConstructor
@Getter
@Setter
public class ProductViewPresenter {

    private final SerializableFunction<ProductEntity, Image> productEntityImageFunction = this::createProductImage;

    private final SerializableFunction<ProductEntity, Component> materialsRenderFunction = this::productMaterialsRender;

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

    public ProductEntity queryProduct(String productName) {
        return this.productEntityRepository.queryFullByName(productName)
                .orElse(null);
    }

    public Component productMaterialsRender(ProductEntity productEntity) {
        List<ProductManufactureInfoEntity> productManufactureInfoEntities = new ArrayList<>(productEntity.getManufactureInfoEntities());
        if (!productManufactureInfoEntities.isEmpty()) {

            if (productManufactureInfoEntities.size() == 1) {
                HorizontalLayout resultComponent = new HorizontalLayout();
                resultComponent.setAlignItems(FlexComponent.Alignment.CENTER);
                resultComponent.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

                productManufactureInfoEntities.stream()
                        .map(this::mapToMaterialCard)
                        .forEach(resultComponent::add);

                return resultComponent;
            } else {
                HorizontalLayout resultComponent = new HorizontalLayout();
                resultComponent.setAlignItems(FlexComponent.Alignment.CENTER);
                resultComponent.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

                productManufactureInfoEntities.stream()
                        .map(this::mapToMaterialAccordion)
                        .forEach(resultComponent::add);

                return resultComponent;
            }
        } else {
            return new Text(productEntity.getBomString());
        }
    }

    public VerticalLayout mapToMaterialCard(ProductManufactureInfoEntity productManufactureInfoEntity) {
        VerticalLayout materialAmountCard = new VerticalLayout();
        Set<ProductMaterialsRelation> materialsRelationSet = productManufactureInfoEntity.getProductMaterialsRelations();
        materialsRelationSet.forEach(productMaterialsRelation -> {
            ProductEntity material = productMaterialsRelation.getMaterial();
            Integer amount = productMaterialsRelation.getAmount();
            HorizontalLayout materialAmountPair =
                    new HorizontalLayout(
                            createProductImage(material),
                            new Text(" x" + amount)
                    );
            materialAmountPair.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            materialAmountCard.add(materialAmountPair);
        });
        return materialAmountCard;
    }

    public Image createProductImage(ProductEntity productEntity) {
        byte[] productImage = productImagesBytesComponent.fetchProductImageBytes(productEntity);
        return ProductImages.productImage(
                productEntity.getName(),
                productImage
        );
    }

    public Accordion mapToMaterialAccordion(ProductManufactureInfoEntity productManufactureInfoEntity) {
        Accordion accordion = new Accordion();
        Set<ProductMaterialsRelation> materialsRelationSet = productManufactureInfoEntity.getProductMaterialsRelations();
        materialsRelationSet.forEach(
                productMaterialsRelation -> {
                    ProductEntity material = productMaterialsRelation.getMaterial();
                    Integer amount = productMaterialsRelation.getAmount();
                    HorizontalLayout materialAmountPair =
                            new HorizontalLayout(
                                    createProductImage(material),
                                    new Text(" x" + amount)
                            );
                    materialAmountPair.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
                    accordion.add(String.valueOf(productManufactureInfoEntity.getId()), materialAmountPair);
                });
        return accordion;
    }

    public List<ProductEntity> getMaterials(ProductEntity productEntity) {
        return this.getMaterialsAsProductManufactureInfo(productEntity)
                .stream()
                .map(ProductManufactureInfoEntity::getProductMaterialsRelations)
                .flatMap(
                        productMaterialsRelations -> productMaterialsRelations.stream()
                                .map(ProductMaterialsRelation::getMaterial)
                )
                .sorted(Comparator.comparingInt(ProductEntity::getLevel)
                        .thenComparing(ProductEntity::getName))
                .toList();
    }

    public Set<ProductManufactureInfoEntity> getMaterialsAsProductManufactureInfo(ProductEntity productEntity) {
        try {
            return productEntity.getManufactureInfoEntities();
        } catch (Exception e) {
            return productManufactureInfoEntityRepository.queryMaterials(productEntity);
        }
    }

    public List<ProductEntity> getComposite(ProductEntity productEntity) {
        return this.productManufactureInfoEntityRepository.queryComposite(productEntity)
                .stream()
                .map(ProductManufactureInfoEntity::getProductEntity)
                .sorted(Comparator.comparingInt(ProductEntity::getLevel)
                        .thenComparing(ProductEntity::getName))
                .toList();
    }

}
