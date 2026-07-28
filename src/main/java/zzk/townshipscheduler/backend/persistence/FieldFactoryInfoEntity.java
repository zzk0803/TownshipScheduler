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
                        value = "productManufactureInfoSet",
                        subgraph = "productManufactureInfoSet.suggraph"
                )
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "productManufactureInfoSet.suggraph",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "productEntity",
                                        subgraph = "productEntity.subgraph"
                                ),
                                @NamedAttributeNode(
                                        value = "productMaterialsRelations",
                                        subgraph = "productMaterialsRelation.subgraph"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "productEntity.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "manufactureInfoEntities"
                                ),
                                @NamedAttributeNode(
                                        value = "crawledAsImage",
                                        subgraph = "crawledAsImage.subgraph"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "crawledAsImage.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(value = "imageBytes")
                        }
                ),
                @NamedSubgraph(
                        name = "productMaterialsRelation.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "material"
                                ),
                                @NamedAttributeNode(
                                        value = "productManufactureInfo"
                                )
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

    private String category;

    private boolean boolCategoryField;

    private Integer level;

    @OneToMany(
            targetEntity = ProductManufactureInfoEntity.class,
            cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}
    )
    @JoinTable(
            name = "jointable_factoryInfo_manufactureInfo",
            joinColumns = @JoinColumn(
                    name = "factoryInfo_id",
                    foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "manufactureinfo_id",
                    foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
            )
    )
    private Set<ProductManufactureInfoEntity> productManufactureInfoSet = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    private ProducingStructureType producingType = ProducingStructureType.QUEUE;

    private Integer defaultInstanceAmount = 1;

    private Integer defaultProducingCapacity = 3;

    private Integer defaultReapWindowCapacity = 6;

    private Integer maxProducingCapacity = 7;

    private Integer maxReapWindowCapacity = 8;

    private Integer maxInstanceAmount = 1;

    public boolean attacheProductManufactureInfoCollection(Collection<? extends ProductManufactureInfoEntity> productManufactureInfoEntities) {
        return productManufactureInfoEntities.stream()
                .allMatch(this::attacheProductManufactureInfo);
    }

    public boolean attacheProductManufactureInfo(ProductManufactureInfoEntity productManufactureInfo) {
        productManufactureInfo.setFieldFactoryInfo(this);
        return this.productManufactureInfoSet.add(productManufactureInfo);
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
        FieldFactoryInfoEntity that = (FieldFactoryInfoEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    //facility method
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

}
