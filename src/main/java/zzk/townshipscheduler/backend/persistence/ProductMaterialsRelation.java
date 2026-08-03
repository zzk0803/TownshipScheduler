package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NamedEntityGraph(
        name = "product-materials.g.full",
        includeAllAttributes = true,
        attributeNodes = {
                @NamedAttributeNode(
                        value = "material",
                        subgraph = "productEntity.suggraph"
                ),
                @NamedAttributeNode(
                        value = "productManufactureInfo"
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
                )
        }
)
public class ProductMaterialsRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private ProductEntity material;

    @ManyToOne
    @JoinColumn(
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private ProductManufactureInfoEntity productManufactureInfo;

    private Integer amount;

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
        ProductMaterialsRelation that = (ProductMaterialsRelation) object;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

}
