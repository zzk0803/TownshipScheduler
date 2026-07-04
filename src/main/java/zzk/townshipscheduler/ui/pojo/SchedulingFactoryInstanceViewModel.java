package zzk.townshipscheduler.ui.pojo;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;

import java.io.Serializable;

/**
 * DTO for {@link SchedulingFactoryInstance}
 */
public record SchedulingFactoryInstanceViewModel(
        Integer id,
        Long fieldFactoryId,
        String categoryName,
        int seqNum,
        int producingLength,
        int reapWindowSize,
        String factoryReadableIdentifier
) implements Serializable {

}
