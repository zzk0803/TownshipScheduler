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

@Entity
@Getter
@Setter
@ToString
public class ProductManufactureInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private ProductEntity productEntity;

    private Duration producingDuration;

    private Integer amountWhenCreated = 1;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "jointable_manufacture_material")
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

    @Override
    public final boolean equals(Object object) {
        if (this == object) return true;
        if (object == null) return false;
        Class<?> oEffectiveClass = object instanceof HibernateProxy
                ? ((HibernateProxy) object).getHibernateLazyInitializer().getPersistentClass()
                : object.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ProductManufactureInfoEntity that = (ProductManufactureInfoEntity) object;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                .hashCode() : getClass().hashCode();
    }

}
