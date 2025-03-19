package zzk.townshipscheduler.backend.scheduling.model;

import lombok.*;
import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.select.*;

import java.util.*;
import java.util.stream.IntStream;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SchedulingFactoryInfo {

    @ToString.Include
    private Id id;

    @ToString.Include
    @EqualsAndHashCode.Include
    private String categoryName;

    private int level;

    private Set<SchedulingProduct> portfolio;

    private SchedulingFactoryInstanceSingle factoryInstance;

    private List<SchedulingFactoryInstanceMultiple> factoryInstances = new ArrayList<>();

    private ProducingStructureType producingStructureType;

    private boolean oneInstance;

    private int defaultInstanceAmount;

    private int defaultProducingCapacity;

    private int defaultReapWindowCapacity;

    private int maxProducingCapacity;

    private int maxReapWindowCapacity;

    private int maxInstanceAmount;

    private int entitySizeEstimated;


    public SchedulingFactoryInfo() {
        this.portfolio = new LinkedHashSet<>();
    }

    public List<ArrangeSequence> toArrangeSequenceValueRange() {
        return IntStream.range(0, entitySizeEstimated)
                .mapToObj(i -> new ArrangeSequence(this.id, i))
                .toList();
    }

    public void appendPortfolioProduct(SchedulingProduct schedulingProduct) {
        this.portfolio.add(schedulingProduct);
    }

    public void appendFactoryInstance(SchedulingFactoryInstanceSingle schedulingFactoryInstanceSingle) {
        this.factoryInstance = schedulingFactoryInstanceSingle;
    }

    public void appendFactoryInstance(SchedulingFactoryInstanceMultiple schedulingFactoryInstanceMultiple) {
        this.factoryInstances.add(schedulingFactoryInstanceMultiple);
    }

    public SchedulingFactoryInstanceSingle getOneFactoryInstance() {
        if (isOneInstance()) {
            return Objects.requireNonNull(this.factoryInstance);
        }
        throw new IllegalStateException();
    }

    @Data
    @AllArgsConstructor
    public static class ArrangeSequence implements Comparable<ArrangeSequence> {

        private Id factoryInfoId;

        private Integer sequence;

        @Override
        public int compareTo(ArrangeSequence that) {
            CompareToBuilder compareToBuilder = new CompareToBuilder();
            compareToBuilder.append(this, that, Comparator.comparingInt(ArrangeSequence::getSequence));
            return compareToBuilder.toComparison();
        }

    }

    @Value
    public static class Id implements Comparable<Id> {

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
