package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.proxy.HibernateProxy;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@Entity
@DynamicUpdate
@NamedEntityGraph(
        name = "warehouse.items",
        attributeNodes = {
                @NamedAttributeNode(
                        value = "warehouseItemEntities",
                        subgraph = "order.items.product"
                )
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "order.items.product",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "product",
                                        subgraph = "products.g.full"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "products.g.full",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "crawledAsImage",
                                        subgraph = "products.g.full.image"
                                ),
                                @NamedAttributeNode("fieldFactoryInfo"),
                                @NamedAttributeNode(
                                        value = "manufactureInfoEntities",
                                        subgraph = "products.g.full.manufacture"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "products.g.full.image",
                        attributeNodes = @NamedAttributeNode("imageBytes")
                ),
                @NamedSubgraph(
                        name = "products.g.full.manufacture",
                        attributeNodes = @NamedAttributeNode("productMaterialsRelations")
                )
        }
)
public class WarehouseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "player_id",
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    @ToString.Exclude
    private PlayerEntity playerEntity;

    @ToString.Exclude
    @OneToMany(
            mappedBy = "warehouse",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private SortedSet<WarehouseItemEntity> warehouseItemEntities = new TreeSet<>();

    @Transient
    private transient Map<ProductEntity, Integer> productAmountMap = new LinkedHashMap<>();

    @PostLoad
    public void postLoad() {
        this.productAmountMap.clear();
        this.productAmountMap.putAll(this.warehouseItemEntities.stream()
                .collect(Collectors.toMap(WarehouseItemEntity::getProduct, WarehouseItemEntity::getAmount)));
    }

    public void removeAllWarehouseItemEntity(Collection<? extends WarehouseItemEntity> warehouseItemEntities) {

        Iterator<? extends WarehouseItemEntity> iterator = warehouseItemEntities.iterator();
        while (iterator.hasNext()) {
            WarehouseItemEntity warehouseItemEntity = iterator.next();
            if (this.warehouseItemEntities.contains(warehouseItemEntity)) {
                removeWarehouseItemEntity(warehouseItemEntity);
                iterator.remove();
            }
        }
    }

    public boolean removeWarehouseItemEntity(WarehouseItemEntity warehouseItemEntity) {
        warehouseItemEntity.setWarehouse(null);
        return warehouseItemEntities.remove(warehouseItemEntity);
    }

    public void doStockAction(ProductEntity product, WarehouseAction action, Integer amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount should more than 0");
        }

        Integer amountInStock = get(product);
        switch (action) {
            case SAVE -> {
                changeProductAmount(product, amountInStock + amount);
            }
            case TAKE -> {
                if (amountInStock <= 0) {
                    return;
                }

                changeProductAmount(product, amountInStock - amount);
            }

            case null, default -> throw new IllegalArgumentException();
        }
    }

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

    public Integer get(ProductEntity product) {
        return productAmountMap.getOrDefault(product, 0);
    }

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

    public void changeProductAmount(ProductEntity product, Integer amount) {
        changeProductAmount(Map.of(product, amount));
    }

    public void changeProductAmount(Map<ProductEntity, Integer> productAmountMap) {
        this.productAmountMap.clear();
        this.productAmountMap.putAll(productAmountMap);
        Set<WarehouseItemEntity> newItems = new TreeSet<>();
        productAmountMap.forEach((productEntity, amount) -> {
            this.warehouseItemEntities.stream()
                    .filter(warehouseItemEntity -> warehouseItemEntity.getProduct().equals(productEntity) && warehouseItemEntity.getWarehouse().equals(this))
                    .findFirst()
                    .ifPresentOrElse(
                            warehouseItemEntity -> warehouseItemEntity.setAmount(amount),
                            () -> {
                                WarehouseItemEntity warehouseItemEntity = new WarehouseItemEntity();
                                warehouseItemEntity.setWarehouse(this);
                                warehouseItemEntity.setProduct(productEntity);
                                warehouseItemEntity.setAmount(amount);
                                newItems.add(warehouseItemEntity);
                            }
                    );
        });
        addAllWarehouseItemEntity(newItems);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        WarehouseEntity that = (WarehouseEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    public void addAllWarehouseItemEntity(Collection<? extends WarehouseItemEntity> warehouseItemEntities) {
        warehouseItemEntities.forEach(this::addWarehouseItemEntity);
    }

    public boolean addWarehouseItemEntity(WarehouseItemEntity warehouseItemEntity) {
        warehouseItemEntity.setWarehouse(this);
        return warehouseItemEntities.add(warehouseItemEntity);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                .hashCode()
                : getClass().hashCode();
    }

    public enum WarehouseAction {
        SAVE,
        TAKE
    }

}
