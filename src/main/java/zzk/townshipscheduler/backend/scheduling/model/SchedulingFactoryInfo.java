package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.select.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SchedulingFactoryInfo {

    @ToString.Include
    @EqualsAndHashCode.Include
    private Id id;

    @ToString.Include
    @EqualsAndHashCode.Include
    private String categoryName;

    private int level;

    private Set<SchedulingProduct> portfolio;

    @ToString.Include
    private ProducingStructureType producingStructureType;

    private int defaultInstanceAmount;

    private int defaultProducingCapacity;

    private int defaultReapWindowCapacity;

    private int maxProducingCapacity;

    private int maxReapWindowCapacity;

    private int maxInstanceAmount;

    public SchedulingFactoryInfo() {
        this.portfolio = new LinkedHashSet<>();
    }

    public void appendPortfolioProduct(SchedulingProduct schedulingProduct) {
        this.portfolio.add(schedulingProduct);
    }

    @Value
    public static class Id {

        long value;

        public static Id of(FieldFactoryInfoEntityDtoJustId factoryDto) {
            return of(factoryDto.getId());
        }

        public static Id of(long value) {
            return new Id(value);
        }

        public static Id of(FieldFactoryInfoEntityDtoForScheduling factoryInfoDto) {
            return of(factoryInfoDto.getId());
        }

        public static Id of(FieldFactoryInfoEntityDto fieldFactoryInfoEntityDto) {
            return of(fieldFactoryInfoEntityDto.getId());
        }

        public static Id of(FieldFactoryInfoEntityProjectionForScheduling fieldFactoryInfoEntityProjectionForScheduling) {
            return of(fieldFactoryInfoEntityProjectionForScheduling.getId());
        }

        public static Id of(FieldFactoryEntityProjection fieldFactoryEntityProjection) {
            return of(fieldFactoryEntityProjection.getId());
        }

        public static Id of(FieldFactoryEntity fieldFactoryEntity) {
            return of(fieldFactoryEntity.getFieldFactoryInfoEntity());
        }

        public static Id of(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
            return of(fieldFactoryInfoEntity.getId());
        }

    }

}
