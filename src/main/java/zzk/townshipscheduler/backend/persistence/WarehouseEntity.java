package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@ToString
@Entity
@DynamicUpdate
@NamedEntityGraph(
        name = "warehouse.product-amount-map",
        attributeNodes = {
                @NamedAttributeNode(
                        value = "productAmountMap",
                        keySubgraph = "warehouse.product-amount-map.key"
                )
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "warehouse.product-amount-map.key",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "crawledAsImage",
                                        subgraph = "warehouse.product-amount-map.key.image"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "warehouse.product-amount-map.key.image",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "imageBytes"
                                )
                        }
                )
        }
)
public class WarehouseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    private PlayerEntity playerEntity;

    @ElementCollection
    @Column(name = "amount")
    @MapKeyJoinColumn(
            name = "product_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    @MapKeyClass(ProductEntity.class)
    private Map<ProductEntity, Integer> productAmountMap =new HashMap<>();

//    @OneToMany(
//            targetEntity = WarehouseRecordEntity.class,
//            cascade = CascadeType.ALL,
//            mappedBy = "warehouseEntity"
//    )
//    @ToString.Exclude
//    private Set<WarehouseRecordEntity> warehouseRecordSet = new TreeSet<>();

//    @OneToOne(
//            targetEntity = WarehouseStocktakeEntity.class,
//            cascade = CascadeType.ALL,
//            mappedBy = "warehouseEntity"
//    )
//    private WarehouseStocktakeEntity warehouseStocktakeEntity;

    public boolean containsProduct(ProductEntity key) {
        return productAmountMap.containsKey(key);
    }

//    @Transient
//    private Map<ProductEntity, Integer> itemAmountMap = new HashMap<>();

//    public boolean appendRecordCollection(Collection<? extends WarehouseRecordEntity> recordEntities) {
//        return recordEntities.stream().map(this::appendRecord).allMatch(boolResult -> boolResult == Boolean.TRUE);
//    }
//
//    public boolean appendRecord(WarehouseRecordEntity warehouseRecordEntity) {
//        warehouseRecordEntity.setWarehouseEntity(this);
//        return warehouseRecordSet.add(warehouseRecordEntity);
//    }

//    @PostLoad
//    public void summarizeItemAmount() {
//        if (Objects.nonNull(warehouseRecordSet) && !warehouseRecordSet.isEmpty()) {
//            warehouseRecordSet.stream()
//                    .collect(Collectors.groupingBy(
//                            WarehouseRecordEntity::getProductEntity,
//                            LinkedHashMap::new,
//                            Collectors.summarizingInt(warehouseRecord -> {
//                                WarehouseRecordEntity.BarnAction barnAction = warehouseRecord.getBarnAction();
//                                Integer amount = warehouseRecord.getAmount();
//                                switch (barnAction) {
//                                    case SAVE -> {
//                                        return amount;
//                                    }
//                                    case TAKE -> {
//                                        return -amount;
//                                    }
//                                    default -> {
//                                        return 0;
//                                    }
//                                }
//                            })
//                    )).forEach((productEntity, intSummaryStatistics) -> {
//                        itemAmountMap.putIfAbsent(productEntity, Math.toIntExact(intSummaryStatistics.getSum()));
//                    });
//        }
//    }

    public Integer get(ProductEntity product) {
        return productAmountMap.getOrDefault(product, 0);
    }

    public Integer setProductAmount(ProductEntity product, Integer value) {
        return productAmountMap.put(product, value);
    }

    public Integer doStockAction(ProductEntity product, WarehouseAction action, Integer amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount should more than 0");
        }

        Integer amountInStock = get(product);
        switch (action) {
            case SAVE -> {
                return setProductAmount(product, amountInStock + amount);
            }
            case TAKE -> {
                if (amountInStock <= 0) {
                    return 0;
                }

                return setProductAmount(product, amountInStock - amount);
            }

            case null, default -> throw new IllegalArgumentException();
        }
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
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        WarehouseEntity that = (WarehouseEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    public enum WarehouseAction {
        SAVE,
        TAKE
    }

}
