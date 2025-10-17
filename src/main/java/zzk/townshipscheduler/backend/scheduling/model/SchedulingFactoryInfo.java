package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import org.javatuples.Pair;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInfo {

    public static final Gatherer<FactoryProcessSequence, FormerCompletedDateTimeRef, Pair<FactoryProcessSequence, FactoryComputedDateTimePair>>
            SLOT_GATHERER = Gatherer.of(
            () -> null,
            (_, arrangement, downstream) -> {

                var arrangeDateTime = arrangement.getArrangeDateTime();
                var producingDuration = arrangement.getProducingDuration();
                if (arrangeDateTime == null) {
                    return true;
                }

                return downstream.push(
                        new Pair<>(
                                arrangement,
                                new FactoryComputedDateTimePair(
                                        arrangeDateTime,
                                        arrangeDateTime.plus(producingDuration)
                                )
                        )
                ) && !downstream.isRejecting();

            },
            Gatherer.defaultCombiner(),
            Gatherer.defaultFinisher()
    );

    public static final Gatherer<FactoryProcessSequence, FormerCompletedDateTimeRef, Pair<FactoryProcessSequence, FactoryComputedDateTimePair>>
            QUEUE_GATHERER = Gatherer.ofSequential(
            FormerCompletedDateTimeRef::new,
            (formerCompletedRef, arrangement, downstream) -> {
                LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
                if (arrangeDateTime == null) {
                    return true;
                }
                LocalDateTime previousCompletedDateTime = formerCompletedRef.value;
                LocalDateTime start = (previousCompletedDateTime == null)
                        ? arrangeDateTime
                        : previousCompletedDateTime.isAfter(
                                arrangeDateTime)
                                ? previousCompletedDateTime
                                : arrangeDateTime;
                LocalDateTime end = start.plus(arrangement.getProducingDuration());
                formerCompletedRef.value = end;
                return downstream.push(
                        new Pair<>(
                                arrangement,
                                new FactoryComputedDateTimePair(
                                        start,
                                        end
                                )
                        )
                ) && !downstream.isRejecting();
            }
    );

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

    @ToString.Include
    @ShadowVariable(supplierName = "supplierForMap")
    private Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, FactoryComputedDateTimePair>> shadowComputedMap = new LinkedHashMap<>();

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
        return this.equals(that) || this.getCategoryName().equals(that.getCategoryName());
    }

    public int calcMaxSupportedProductDurationMinutes() {
        if (this.maxSupportedProductDurationMinutes != null) {
            return this.maxSupportedProductDurationMinutes;
        }

        return this.maxSupportedProductDurationMinutes = getPortfolio().stream()
                .mapToInt(
                        product -> product.getExecutionModeSet().stream()
                                .map(schedulingProducingExecutionMode -> Math.toIntExact(
                                        schedulingProducingExecutionMode.getExecuteDuration().toMinutes()))
                                .min(Comparator.naturalOrder())
                                .orElseThrow(() -> new NoSuchElementException("product %s,execution mode %s".formatted(
                                                product.getName(),
                                                product.getExecutionModeSet().toString()
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

    @ShadowSources({"factoryInstances[].factoryProcessSequenceList"})
    public Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, FactoryComputedDateTimePair>> supplierForMap() {
        return this.factoryInstances.stream()
                .collect(
                        Collectors.toMap(
                                SchedulingFactoryInstance::getFactoryReadableIdentifier,
                                factoryInstance -> {
                                    return factoryInstance.getFactoryProcessSequenceList().stream()
                                            .sorted(FactoryProcessSequence.COMPARATOR)
                                            .gather(this.weatherFactoryProducingTypeIsQueue()
                                                    ? QUEUE_GATHERER
                                                    : SLOT_GATHERER
                                            )
                                            .collect(
                                                    LinkedHashMap::new,
                                                    (treeMap, pair) -> treeMap.put(
                                                            pair.getValue0(),
                                                            pair.getValue1()
                                                    ),
                                                    LinkedHashMap::putAll
                                            );
                                }
                        )
                );
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.producingStructureType == ProducingStructureType.QUEUE;
    }

    public boolean weatherFactoryProducingTypeIsSlot() {
        return this.producingStructureType == ProducingStructureType.SLOT;
    }

    public FactoryComputedDateTimePair query(SchedulingProducingArrangement schedulingProducingArrangement) {
        FactoryProcessSequence factoryProcessSequence = schedulingProducingArrangement.getFactoryProcessSequence();
        Map<FactoryProcessSequence, FactoryComputedDateTimePair> computedDateTimePairMap
                = this.shadowComputedMap.get(factoryProcessSequence.getFactoryReadableIdentifier());
        if (computedDateTimePairMap == null) {
            return null;
        }
        return computedDateTimePairMap.get(factoryProcessSequence);
    }

    @Value
    public static class Id implements Comparable<Id> {

        @JsonProperty("id")
        @Getter
        private long value;

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

    public static class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }


}
