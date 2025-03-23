package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Duration;
import java.util.*;
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

    public List<BaseProducingArrangement> materialsActionsDeep() {
        ArrayList<BaseProducingArrangement> materialArrangements = new ArrayList<>();
        LinkedList<BaseProducingArrangement> dealingChain = new LinkedList<>(materialsActions());

        while (!dealingChain.isEmpty()) {
            BaseProducingArrangement currentArrangement = dealingChain.removeFirst();

            Set<SchedulingProducingExecutionMode> executionModes
                    = currentArrangement.getCurrentActionObject().getExecutionModeSet();
            SchedulingProducingExecutionMode producingExecutionMode
                    = executionModes.stream()
                    .min(Comparator.comparing(SchedulingProducingExecutionMode::getExecuteDuration))
                    .orElseThrow();
            currentArrangement.setProducingExecutionMode(producingExecutionMode);

            List<BaseProducingArrangement> materialsActions = producingExecutionMode.materialsActions();
            materialsActions.forEach(dealingChain::addLast);

        }

        return materialArrangements;
    }

    public List<BaseProducingArrangement> materialsActions() {
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
