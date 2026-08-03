package zzk.townshipscheduler.ui.pojo.scheduling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ProductAmountBillViewModel(
        Collection<SchedulingProductAmountPair> productAmountPairs
) {

    public ProductAmountBillViewModel merge(ProductAmountBillViewModel thatProductAmountBillViewModel) {
        return new ProductAmountBillViewModel(
                Stream.of(
                                this.productAmountPairs.stream(),
                                thatProductAmountBillViewModel.productAmountPairs.stream()
                        )
                        .flatMap(Function.identity())
                        .collect(
                                Collectors.collectingAndThen(
                                        Collectors.toMap(
                                                SchedulingProductAmountPair::product,
                                                SchedulingProductAmountPair::amount,
                                                Integer::sum
                                        ),
                                        schedulingProductAmountMap -> {
                                            return schedulingProductAmountMap.entrySet()
                                                    .stream()
                                                    .map(schedulingProductViewModelIntegerEntry -> {
                                                        SchedulingProductViewModel schedulingProductViewModel = schedulingProductViewModelIntegerEntry.getKey();
                                                        Integer amount = schedulingProductViewModelIntegerEntry.getValue();
                                                        return new SchedulingProductAmountPair(
                                                                schedulingProductViewModel,
                                                                amount
                                                        );
                                                    })
                                                    .collect(Collectors.toCollection(ArrayList::new));
                                        }
                                )
                        )
        );
    }

}
