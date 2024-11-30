package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.*;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;

@PlanningEntity
@Data
@ToString(of = {"uid", "schedulingProduct"})
@EqualsAndHashCode(of = "uid")
@JsonIdentityInfo(
        scope = SchedulingProducing.class,
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "uid"
)
public class SchedulingProducing {

    @PlanningId
    private String uid = UUID.randomUUID().toString();

    private  SchedulingProduct schedulingProduct;

    @InverseRelationShadowVariable(sourceVariableName = "schedulingProducingList")
    private SchedulingFactorySlot schedulingFactorySlot;

    @IndexShadowVariable(sourceVariableName = "schedulingProducingList")
    private Integer producingIndex;

    @PreviousElementShadowVariable(sourceVariableName = "schedulingProducingList")
    private SchedulingProducing previousProducing;

    @NextElementShadowVariable(sourceVariableName = "schedulingProducingList")
    private SchedulingProducing nextProducing;

    @CascadingUpdateShadowVariable(targetMethodName = "schedulingProducingShadowUpdate")
    private LocalDateTime arrangeDateTime;

    @CascadingUpdateShadowVariable(targetMethodName = "schedulingProducingShadowUpdate")
    private LocalDateTime producingInGameDateTime;

    @CascadingUpdateShadowVariable(targetMethodName = "schedulingProducingShadowUpdate")
    private LocalDateTime completedInGameDateTime;

    @JsonIgnore
    @CascadingUpdateShadowVariable(targetMethodName = "schedulingProducingShadowUpdate")
    private LinkedHashMap<SchedulingProduct, Integer> warehouse = new LinkedHashMap<>();

    public void schedulingProducingShadowUpdate() {
//        getSchedulingProduct().getMaterialAmountMap().forEach((sp, i) -> {
//            warehouse.compute(sp, ((spInMap, iInMap) -> warehouse.getOrDefault(spInMap, 0) - i));
//        });
//
//        //deduce arrange datetime,producing datetime and complete datetime
//        if (getPreviousProducing() == null) {
//            setArrangeDateTime(LocalDateTime.now());
//            setProducingInGameDateTime(getArrangeDateTime());
//            setCompletedInGameDateTime(this.getArrangeDateTime().plus(getSchedulingProduct().getProducingDuration()));
//        } else {
//            warehouse.compute(getPreviousProducing().getSchedulingProduct(), (sp, i) -> i + sp.getGainWhenCompleted());
//            setArrangeDateTime(getPreviousProducing().getCompletedInGameDateTime());
//            setProducingInGameDateTime(getPreviousProducing().getCompletedInGameDateTime());
//            setCompletedInGameDateTime(
//                    getPreviousProducing().getProducingInGameDateTime()
//                            .plus(getSchedulingProduct().getProducingDuration())
//            );
//        }
    }

}
