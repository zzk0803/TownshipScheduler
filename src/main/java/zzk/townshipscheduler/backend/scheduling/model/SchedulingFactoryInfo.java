package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.persistence.select.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public  class SchedulingFactoryInfo  {

    @EqualsAndHashCode.Include
    private Id id;

    @EqualsAndHashCode.Include
    private String categoryName;

    private int level;

    private Set<SchedulingProduct> portfolio;

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

    @Override
    public String toString() {
        return "{\"SchedulingFactoryInfo\":{"
               + "        \"id\":" + id
               + ",         \"categoryName\":\"" + categoryName + "\""
               + ",         \"portfolio\":" + portfolio.stream().map(SchedulingProduct::getName).collect(Collectors.joining(","))
               + ",         \"producingType\":\"" + producingStructureType + "\""
               + "}}";
    }

    @Value
    public static class Id{

         long value;

        public static Id of(long value) {
            return new Id(value);
        }

        public static Id of(FieldFactoryInfoEntityDtoJustId factoryDto) {
            return of(factoryDto.getId());
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

        public static Id of(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
            return of(fieldFactoryInfoEntity.getId());
        }

        public static Id of(FieldFactoryEntity fieldFactoryEntity) {
            return of(fieldFactoryEntity.getFieldFactoryInfoEntity());
        }

    }

}
