package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@ToString
@NamedEntityGraph(
        name = "product-manufacture-info.g.full",
        includeAllAttributes = true,
        attributeNodes = {
                @NamedAttributeNode(
                        value = "productEntity",
                        subgraph = "productEntity.suggraph"
                ),
                @NamedAttributeNode(
                        value = "productMaterialsRelations",
                        subgraph = "productMaterialsRelation.subgraph"
                ),
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "productEntity.suggraph",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "crawledAsImage",
                                        subgraph = "wikiCrawledEntity.subgraph"
                                ),
                                @NamedAttributeNode(
                                        value = "manufactureInfoEntities"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "wikiCrawledEntity.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(value = "imageBytes")
                        }
                ),
                @NamedSubgraph(
                        name = "productMaterialsRelation.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(value = "productManufactureInfo"),
                                @NamedAttributeNode(value = "material")
                        }
                )
        }
)
public class ProductManufactureInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Duration producingDuration = Duration.ZERO;

    private Integer amountWhenCreated = 1;

    @ManyToOne
    @JoinColumn(
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private ProductEntity productEntity;

    @ManyToOne
    private FieldFactoryInfoEntity fieldFactoryInfo;

    @OneToMany(mappedBy = "productManufactureInfo")
    @ToString.Exclude
    private Set<ProductMaterialsRelation> productMaterialsRelations = new HashSet<>();

    public boolean attacheProductMaterialsRelation(ProductMaterialsRelation productMaterialsRelation) {
        productMaterialsRelation.setProductManufactureInfo(this);
        return productMaterialsRelations.add(productMaterialsRelation);
    }

    public boolean detachProductMaterialsRelation(ProductMaterialsRelation productMaterialsRelation) {
        productMaterialsRelation.setProductManufactureInfo(null);
        return productMaterialsRelations.remove(productMaterialsRelation);
    }

    public ProductAmountBill toProductAmountBill() {
        return ProductAmountBill.of(productMaterialsRelations.stream()
                .collect(Collectors.toMap(
                                ProductMaterialsRelation::getMaterial,
                                ProductMaterialsRelation::getAmount,
                                Integer::sum
                        )
                ));
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                .hashCode()
                : getClass().hashCode();
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null)
            return false;
        Class<?> oEffectiveClass = object instanceof HibernateProxy
                ? ((HibernateProxy) object).getHibernateLazyInitializer().getPersistentClass()
                : object.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        ProductManufactureInfoEntity that = (ProductManufactureInfoEntity) object;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

}
