package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.select.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SchedulingFactoryInfo {

    @JsonUnwrapped
    @ToString.Include
    @EqualsAndHashCode.Include
    private Id id;

    @ToString.Include
    @EqualsAndHashCode.Include
    private String categoryName;

    private int level;

    @JsonBackReference
    private List<SchedulingProduct> portfolio;

    @JsonBackReference
    private List<SchedulingFactoryInstance> factoryInstances;

    private ProducingStructureType producingStructureType;

    private int defaultInstanceAmount;

    private int defaultProducingCapacity;

    private int defaultReapWindowCapacity;

    private int maxProducingCapacity;

    private int maxReapWindowCapacity;

    private int maxInstanceAmount;

    public SchedulingFactoryInfo() {
        this.portfolio = new ArrayList<>();
        this.factoryInstances = new ArrayList<>();
    }

    public void appendPortfolioProduct(SchedulingProduct schedulingProduct) {
        this.portfolio.add(schedulingProduct);
    }

    public boolean typeEqual(SchedulingFactoryInfo that) {
        return this.getCategoryName().equals(that.getCategoryName());
    }

    @Value
    public static class Id implements Comparable<Id> {

        @JsonProperty("id")
        @Getter
        private long value;

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

        @Override
        public int compareTo(Id that) {
            Comparator<Id> comparator = Comparator.comparing(Id::getValue);
            return comparator.compare(this, that);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

    }

}
