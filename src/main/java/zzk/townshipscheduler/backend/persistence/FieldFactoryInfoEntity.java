package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@NamedEntityGraph(
        name = "fieldFactoryInfo.g.portfolioGoods",
        attributeNodes = {
                @NamedAttributeNode("portfolioGoods")
        }
)
public class FieldFactoryInfoEntity {

    public static final String FIELD_CATEGORY_CRITERIA = "Field";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;

    private boolean boolCategoryField;

    private Integer level;

    @OneToMany(
            cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
            targetEntity = ProductEntity.class,
            mappedBy = "fieldFactoryInfo"
    )
    @ToString.Exclude
    @Builder.Default
    private Set<ProductEntity> portfolioGoods = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProducingStructureType producingType = ProducingStructureType.QUEUE;

    @Builder.Default
    private Integer defaultInstanceAmount = 1;

    @Builder.Default
    private Integer defaultProducingCapacity = 3;

    @Builder.Default
    private Integer defaultReapWindowCapacity = 6;

    @Builder.Default
    private Integer maxProducingCapacity = 7;

    @Builder.Default
    private Integer maxReapWindowCapacity = 8;

    @Builder.Default
    private Integer maxInstanceAmount = 1;

    public void attacheProductEntity(ProductEntity productEntity) {
        productEntity.setFieldFactoryInfo(this);
        portfolioGoods.add(productEntity);
    }

    public void attacheProductEntities(Collection<ProductEntity> productEntities) {
        if (productEntities == null || productEntities.isEmpty()) {
            return;
        }
        productEntities.forEach(product -> {
            product.setFieldFactoryInfo(this);
            this.attacheProductEntity(product);
        });
    }

    public boolean detachProductEntity(ProductEntity productEntity) {
        productEntity.setFieldFactoryInfo(null);
        return portfolioGoods.remove(productEntity);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                .hashCode() : getClass().hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer()
                .getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
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
