package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.stream.IntStream;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
//@PlanningEntity
@ToString(onlyExplicitlyIncluded = true)
public class SchedulingProducingExecutionMode implements Serializable {

    @Serial
    private static final long serialVersionUID = 3492377670290631633L;

    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    private Long productManufactureInfoId;

    private SchedulingProduct product;

    @ToString.Include
    private ProductAmountBill materials;

    @ToString.Include
    private Duration executeDuration = Duration.ZERO;

//    @InverseRelationShadowVariable(sourceVariableName = "planningProducingExecutionMode")
//    private List<SchedulingPlayerFactoryAction> assignedProducingActionSet = new ArrayList<>();

    public SchedulingProducingExecutionMode() {
    }

    public List<SchedulingProducingArrangement> materialsActions() {
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

    public boolean boolCompositeProduct() {
        return !boolAtomicProduct();
    }


}
