package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
//@PlanningEntity
@ToString(onlyExplicitlyIncluded = true)
public class SchedulingProducingExecutionMode {

    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    private SchedulingProduct product;

    @ToString.Include
    private ProductAmountBill materials;

    @ToString.Include
    private Duration executeDuration = Duration.ZERO;

//    @InverseRelationShadowVariable(sourceVariableName = "planningProducingExecutionMode")
//    private List<SchedulingPlayerFactoryAction> assignedProducingActionSet = new ArrayList<>();

    public SchedulingProducingExecutionMode() {
    }

    public List<SchedulingPlayerFactoryAction> materialsActions() {
        return boolAtomicProduct()
                ? List.of()
                : materials.entrySet().stream()
                        .flatMap(entry -> {
                            SchedulingProduct schedulingProduct = entry.getKey();
                            int amount = entry.getValue();
                            return IntStream.range(0, amount)
                                    .mapToObj(_ -> schedulingProduct.calcFactoryActions(getProduct()))
                                    .flatMap(Collection::stream);
                        })
                        .toList();
    }

    public boolean boolAtomicProduct() {
        return materials == null || materials.isEmpty();
    }


}
