package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SchedulingFactoryInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = -3411137456098907358L;

    @JsonUnwrapped
    @EqualsAndHashCode.Include
    private Id id;

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

    private Integer maxSupportedProductDurationMinutes;

    public SchedulingFactoryInfo() {
        this.portfolio = new ArrayList<>();
        this.factoryInstances = new ArrayList<>();
    }

    public void appendPortfolioProduct(SchedulingProduct schedulingProduct) {
        this.portfolio.add(Objects.requireNonNull(schedulingProduct));
    }

    public void add(SchedulingFactoryInstance schedulingFactoryInstance) {
        this.factoryInstances.add(Objects.requireNonNull(schedulingFactoryInstance));
    }

    public boolean typeEqual(SchedulingFactoryInfo that) {
        return this.equals(that) || this.getCategoryName()
                .equals(that.getCategoryName());
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.producingStructureType == ProducingStructureType.QUEUE;
    }

    public int calcMaxSupportedProductDurationMinutes() {
        if (this.maxSupportedProductDurationMinutes != null) {
            return this.maxSupportedProductDurationMinutes;
        }

        return this.maxSupportedProductDurationMinutes = getPortfolio().stream()
                .mapToInt(
                        product -> product.getExecutionModeSet()
                                .stream()
                                .map(schedulingProducingExecutionMode -> Math.toIntExact(
                                        schedulingProducingExecutionMode.getExecuteDuration()
                                                .toMinutes()))
                                .min(Comparator.naturalOrder())
                                .orElseThrow(() -> new NoSuchElementException("product %s,execution mode %s".formatted(
                                                product.getName(),
                                                product.getExecutionModeSet()
                                                        .toString()
                                        ))
                                )
                )
                .max()
                .orElse(60);
    }

    @Override
    public String toString() {
        return "SchedulingFactoryInfo{" +
                "id=" + id +
                ", categoryName='" + categoryName + '\'' +
                ", level=" + level +
                ", portfolio=" + portfolio.stream()
                .map(SchedulingProduct::getName)
                .collect(Collectors.joining(",", "[", "]")) +
                ", producingStructureType=" + producingStructureType +
                ", factoryInstances=" + factoryInstances.stream()
                .map(SchedulingFactoryInstance::getFactoryReadableIdentifier)
                .collect(
                        Collectors.joining(",", "[", "]")) +
                '}';
    }

    @Value
    public static class Id implements Comparable<Id>, Serializable {

        @Serial
        private static final long serialVersionUID = 2278621691459222451L;

        @JsonProperty("id")
        @Getter
        long value;

        public static Id of(FieldFactoryEntity fieldFactoryEntity) {
            return of(fieldFactoryEntity.getFieldFactoryInfoEntity());
        }

        public static Id of(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
            return of(fieldFactoryInfoEntity.getId());
        }

        public static Id of(Long value) {
            return new Id(value);
        }

        public static Id of(long value) {
            return new Id(value);
        }

        @Override
        public int compareTo(Id that) {
            return Long.compare(this.value, that.value);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(getValue());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Id id)) return false;

            return getValue() == id.getValue();
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

    }

}
