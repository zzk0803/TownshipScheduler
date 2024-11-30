package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import lombok.EqualsAndHashCode;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.pojo.projection.ProductEntityForSchedulingDto;

import java.util.List;

@Data
@JsonIdentityInfo(
        scope = SchedulingProduct.class,
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id"
)
public class SchedulingProduct {

    @EqualsAndHashCode.Include
    private long id;

    private String productName;

    private String category;

    private Integer level;

    private  boolean atomicGoods;

    private String bomString;

    private String durationString;

    private Integer gainWhenCompleted;

    public SchedulingProduct(ProductEntity productEntity) {
        this.id = productEntity.getId();
        this.productName = productEntity.getName();
        this.category = productEntity.getCategory();
        this.level = productEntity.getLevel();
        this.bomString = productEntity.getBomString();
        this.durationString = productEntity.getDurationString();
        this.gainWhenCompleted = productEntity.getGainWhenCompleted();
    }

    public SchedulingProduct(ProductEntityForSchedulingDto productEntityDto) {
        this.id = productEntityDto.getId();
        this.productName = productEntityDto.getName();
        this.category = productEntityDto.getCategory();
        this.level = productEntityDto.getLevel();
        this.bomString = productEntityDto.getBomString();
        this.durationString = productEntityDto.getDurationString();
        this.gainWhenCompleted = productEntityDto.getGainWhenCompleted();
    }

    public boolean boolAtomicGoods() {
//        this.atomicGoods = this.productManufactureRelation == null || this.productManufactureRelation.boolAtomicProduct();
        return this.atomicGoods;
    }

    public List<SchedulingProducing> calcProducingGoods() {
//        if (boolAtomicGoods()) {
//            SchedulingProducing schedulingProducing = new SchedulingProducing();
//            schedulingProducing.setSchedulingProduct(this);
//            return Collections.singletonList(schedulingProducing);
//        }
//
//        List<SchedulingProducing> result = new ArrayList<>();
//        materialAmountMap.forEach((materialGoods, amount) -> {
//            for (int i = 0; i < amount; i++) {
//                result.addAll(materialGoods.calcProducingGoods());
//            }
//        });
//        SchedulingProducing schedulingProducing = new SchedulingProducing();
//        schedulingProducing.setSchedulingProduct(this);
//        result.add(schedulingProducing);
//        return result;
        return List.of();
    }

}
