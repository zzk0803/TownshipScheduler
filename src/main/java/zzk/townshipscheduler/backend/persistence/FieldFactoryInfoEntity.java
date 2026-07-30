package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@NamedEntityGraph(
        name = "fieldFactoryInfo.g.full",
        includeAllAttributes = true,
        attributeNodes = {
                @NamedAttributeNode(
                        value = "productEntities",
                        subgraph = "productEntities.suggraph"
                )
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "productEntities.suggraph",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "manufactureInfoEntities",
                                        subgraph = "manufactureInfoEntities.subgraph"
                                ),
                                @NamedAttributeNode(
                                        value = "crawledAsImage",
                                        subgraph = "crawledAsImage.subgraph"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "manufactureInfoEntities.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(value = "productEntity"),
                                @NamedAttributeNode(value = "productMaterialsRelations")
                        }
                ),
                @NamedSubgraph(
                        name = "crawledAsImage.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(value = "imageBytes")
                        }
                )
        }
)
public class FieldFactoryInfoEntity {

    public static final FieldFactoryInfoEntity NULL_EMPTY_VALUE = new FieldFactoryInfoEntity() {{
        setId(-1L);
        setCategory("N/A");
        setLevel(-1);
    }};

    public static final String FIELD_CATEGORY_CRITERIA = "Field";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String category;

    private boolean boolCategoryField;

    private Integer level;

    @Enumerated(EnumType.STRING)
    private ProducingStructureType producingType = ProducingStructureType.QUEUE;

    private Integer defaultInstanceAmount = 1;

    private Integer defaultProducingCapacity = 3;

    private Integer defaultReapWindowCapacity = 6;

    private Integer maxProducingCapacity = 7;

    private Integer maxReapWindowCapacity = 8;

    private Integer maxInstanceAmount = 1;

    @OneToMany(
            mappedBy = "fieldFactoryInfoEntity",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH}
    ) private Set<ProductEntity> productEntities = new LinkedHashSet<>();

    public boolean removeProductEntity(ProductEntity productEntity) {
        if (!this.getProductEntities()
                .contains(productEntity)) {
            return false;
        }
        productEntity.setFieldFactoryInfoEntity(null);
        return productEntities.remove(productEntity);
    }

    public synchronized void clearProductEntity() {
        for (ProductEntity productEntity : productEntities) {
            productEntity.setFieldFactoryInfoEntity(null);
        }
        this.productEntities.clear();
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
                ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        FieldFactoryInfoEntity that = (FieldFactoryInfoEntity) o;
        return (getId() != null && Objects.equals(getId(), that.getId()))
               || (getCategory() != null && Objects.equals(getCategory(), that.getCategory()));
    }

    public FieldFactoryEntity toFieldFactoryEntity() {
        FieldFactoryEntity fieldFactoryEntity = new FieldFactoryEntity();
        fieldFactoryEntity.setFieldFactoryInfoEntity(this);
        fieldFactoryEntity.setProducingLength(this.getDefaultProducingCapacity());
        fieldFactoryEntity.setReapWindowSize(this.getDefaultReapWindowCapacity());
        return fieldFactoryEntity;
    }

    public FieldFactoryEntity toFieldFactoryEntity(Supplier<PlayerEntity> playerEntitySupplier) {
        FieldFactoryEntity fieldFactoryEntity = new FieldFactoryEntity(this, playerEntitySupplier.get());
        fieldFactoryEntity.setProducingLength(this.getDefaultProducingCapacity());
        fieldFactoryEntity.setReapWindowSize(this.getDefaultReapWindowCapacity());
        return fieldFactoryEntity;
    }

    public void addProductEntities(Collection<ProductEntity> productEntities) {
        productEntities.forEach(this::addProductEntity);
    }

    public boolean addProductEntity(ProductEntity productEntity) {
        productEntity.setFieldFactoryInfoEntity(this);
        return productEntities.add(productEntity);
    }

}
