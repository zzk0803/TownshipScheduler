package zzk.townshipscheduler.ui.pojo.scheduling;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingProduct;

import java.io.Serializable;

/**
 * DTO for {@link SchedulingProduct}
 */
public record SchedulingProductViewModel(
        SchedulingProductViewModelId id,
        String name,
        int level,
        int gainWhenCompleted
) implements Serializable {
    public static record SchedulingProductViewModelId(long id)implements Serializable{

        public static SchedulingProductViewModelId of(long id) {
            return new SchedulingProductViewModelId(id);
        }
    }

}
