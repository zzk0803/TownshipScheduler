package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.proxy.HibernateProxy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@DynamicInsert
@NamedEntityGraph(
        name = "products.g.full",
        includeAllAttributes = true,
        attributeNodes = {
                @NamedAttributeNode(
                        value = "crawledAsImage",
                        subgraph = "crawledAsImage.subgraph"
                ),
                @NamedAttributeNode(
                        value = "manufactureInfoEntities",
                        subgraph = "manufactureInfoEntities.subgraph"
                )
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "crawledAsImage.subgraph",
                        attributeNodes = @NamedAttributeNode("imageBytes")
                ),
                @NamedSubgraph(
                        name = "manufactureInfoEntities.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "fieldFactoryInfo",
                                        subgraph = "fieldFactoryInfo.subgraph"
                                ),
                                @NamedAttributeNode(
                                        value = "productMaterialsRelations",
                                        subgraph = "productMaterialsRelations.subgraph"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "fieldFactoryInfo.subgraph",
                        attributeNodes = @NamedAttributeNode("productManufactureInfoSet")
                ),
                @NamedSubgraph(
                        name = "productMaterialsRelations.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode("material"),
                                @NamedAttributeNode("productManufactureInfo"),
                        }
                )
        }
)
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private transient ProductId productId;

    private String name = "";

    private String nameForMaterial = "";

    private String category = "";

    private Integer level = 1;

    private Integer cost = 0;

    private Integer sellPrice = 0;

    private Integer xp = 0;

    private Integer dealerValue = 0;

    private Integer helpValue = 0;

    private Integer defaultAmountWhenCreated = 1;

    private String bomString = "";

    private String durationString = "";

    @OneToMany(mappedBy = "productEntity")
    private Set<ProductManufactureInfoEntity> manufactureInfoEntities = new HashSet<>();

    @OneToOne
    private WikiCrawledEntity crawledAsImage;

    @PostLoad
    public void postLoad() {
        setProductId(ProductId.of(getId()));
    }

    public boolean attacheProductManufactureInfo(ProductManufactureInfoEntity productManufactureInfo) {
        productManufactureInfo.setProductEntity(this);
        return manufactureInfoEntities.add(productManufactureInfo);
    }

    public boolean attacheProductManufactureInfoCollection(Collection<? extends ProductManufactureInfoEntity> productManufactureInfoEntities) {
        return productManufactureInfoEntities.stream()
                .allMatch(this::attacheProductManufactureInfo);
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
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer()
                .getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        ProductEntity productEntity = (ProductEntity) o;
        return getId() != null && Objects.equals(getId(), productEntity.getId());
    }

    public ProductId getProductId() {
        return this.productId == null
                ? this.productId = new ProductId(this.getId())
                : this.productId;
    }

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductId {

        private Long value;

        public static ProductId of(long value) {
            return new ProductId(value);
        }

    }

}
