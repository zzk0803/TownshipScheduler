package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;


@Data
@EqualsAndHashCode(exclude = {"factorySlotList"})
@RequiredArgsConstructor
@JsonIdentityInfo(
        scope = SchedulingFactory.class,
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "category"
)
public class SchedulingFactory {

    private final String category;

    @JsonIdentityReference(alwaysAsId = true)
    private final List<SchedulingProduct> portfolioProductList;

    private final int slotAmount;

    @JsonIdentityReference(alwaysAsId = true)
    private List<SchedulingFactorySlot> factorySlotList;

}
