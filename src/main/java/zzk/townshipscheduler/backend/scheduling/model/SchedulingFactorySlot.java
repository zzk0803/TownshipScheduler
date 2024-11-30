package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@ToString(of = "id")
@PlanningEntity
@EqualsAndHashCode(exclude = "schedulingProducingList")
@JsonIdentityInfo(
        scope = SchedulingProducing.class,
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id"
)
public class SchedulingFactorySlot {

    @PlanningId
    private  String id;

    @JsonIdentityReference
    private  SchedulingFactory schedulingFactory;

    private  SchedulingGamePlayer player;

    @JsonIdentityReference(alwaysAsId = true)
    @PlanningListVariable(allowsUnassignedValues = true)
    private List<SchedulingProducing> schedulingProducingList;

    //illegal
    //    @ValueRangeProvider(id = "valueRangeSchedulingProducing")
    //    public List<SchedulingProducing> valueRangeSchedulingProducing() {
    //        return player.getSchedulingProducingList().stream()
    //                .filter(schedulingProducing -> schedulingProducing.getSchedulingFactory() == this.getSchedulingFactory())
    //                .toList();
    //    }

    public String getCategory() {
        return schedulingFactory.getCategory();
    }

    public List<SchedulingProduct> getPortfolioProductList() {
        return schedulingFactory.getPortfolioProductList();
    }

    public int getSlotAmount() {
        return schedulingFactory.getSlotAmount();
    }


}
