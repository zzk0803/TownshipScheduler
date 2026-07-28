package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.proxy.HibernateProxy;

import java.util.*;

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

    @OneToMany(
            targetEntity = ProductManufactureInfoEntity.class,
            cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE}
    )
    @JoinTable(
            name = "jointable_product_manufactureInfo",
            joinColumns = @JoinColumn(
                    name = "product_id",
                    foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "manufactureInfo_id",
                    foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
            )
    )
    private Set<ProductManufactureInfoEntity> manufactureInfoEntities = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private WikiCrawledEntity crawledAsImage;

    @PostLoad
    public void postLoad() {
        setProductId(ProductId.of(getId()));
    }

    public boolean attacheProductManufactureInfoCollection(Collection<? extends ProductManufactureInfoEntity> productManufactureInfoEntities) {
        return productManufactureInfoEntities.stream()
                .allMatch(this::attacheProductManufactureInfo);
    }

    public boolean attacheProductManufactureInfo(ProductManufactureInfoEntity productManufactureInfo) {
        productManufactureInfo.setProductEntity(this);
        return manufactureInfoEntities.add(productManufactureInfo);
    }

    public void detachProductManufactureInfoCollection(Collection<? extends ProductManufactureInfoEntity> productManufactureInfoEntities) {
        Iterator<? extends ProductManufactureInfoEntity> iterator = productManufactureInfoEntities.iterator();
        while (iterator.hasNext()) {
            ProductManufactureInfoEntity productManufactureInfoEntity = iterator.next();
            this.detachProductManufactureInfo(productManufactureInfoEntity);
            iterator.remove();
        }
    }

    public boolean detachProductManufactureInfo(ProductManufactureInfoEntity productManufactureInfo) {
        if (this.manufactureInfoEntities.contains(productManufactureInfo)) {
            productManufactureInfo.setFieldFactoryInfo(null);
        }
        return this.manufactureInfoEntities.remove(productManufactureInfo);
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductId {

        private Long value;

        public static ProductId of(long value) {
            return new ProductId(value);
        }

    }

}
