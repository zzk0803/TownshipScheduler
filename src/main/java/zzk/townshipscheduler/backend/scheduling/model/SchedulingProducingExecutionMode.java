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

    public List<AbstractPlayerProducingArrangement> materialsActions() {
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

    public List<AbstractPlayerProducingArrangement> materialsActionsDeep() {
        LinkedList<AbstractPlayerProducingArrangement> dealingChain = new LinkedList<>(materialsActions());
        ArrayList<AbstractPlayerProducingArrangement> factoryActions = new ArrayList<>();

        while (!dealingChain.isEmpty()) {
            AbstractPlayerProducingArrangement currentFactoryAction = dealingChain.removeFirst();

            Set<SchedulingProducingExecutionMode> executionModes
                    = currentFactoryAction.getCurrentActionObject().getExecutionModeSet();
            SchedulingProducingExecutionMode producingExecutionMode
                    = executionModes.stream()
                    .min(Comparator.comparing(SchedulingProducingExecutionMode::getExecuteDuration))
                    .orElseThrow();
            currentFactoryAction.setProducingExecutionMode(producingExecutionMode);

            List<AbstractPlayerProducingArrangement> materialsActions = producingExecutionMode.materialsActions();
            materialsActions.forEach(dealingChain::addLast);

        }

        return factoryActions;
    }

    public boolean boolAtomicProduct() {
        return materials == null || materials.isEmpty();
    }

    public boolean boolCompositeProduct() {
        return !boolAtomicProduct();
    }


}
