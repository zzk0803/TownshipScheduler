package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@PlanningEntity
public class SchedulingFactoryInstance {

    @PlanningId
    private Integer id;

    private SchedulingFactoryInfo factoryInfo;

    @InverseRelationShadowVariable(sourceVariableName = "planningFactoryInstance")
    private List<SchedulingGameActionProductProducing> assignedActionInFactory;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    public SchedulingFactoryInstance(
            SchedulingFactoryInfo factoryInfo,
            int seqNum,
            int producingLength,
            int reapWindowSize
    ) {
        this.factoryInfo = factoryInfo;
        this.seqNum = seqNum;
        this.producingLength = producingLength;
        this.reapWindowSize = reapWindowSize;
    }

    public void shadowUpdate() {
        Map<SchedulingDateTimeSlot, List<SchedulingGameActionProductProducing>> timeSlotToFactoryActionMap =
                assignedActionInFactory.stream()
                        .collect(Collectors.groupingBy(
                                SchedulingGameAction::getPlanningDateTimeSlot)
                        );

        SchedulingGameActionProductProducing previousIteratedAction = null;
        LocalDateTime previousIteratedActionProducingDateTime = null;
        LocalDateTime previousIteratedActionCompletedDateTime = null;
        for (Map.Entry<SchedulingDateTimeSlot, List<SchedulingGameActionProductProducing>> entry
                : timeSlotToFactoryActionMap.entrySet()
        ) {
            SchedulingDateTimeSlot timeSlot = entry.getKey();
            List<SchedulingGameActionProductProducing> actionSet = entry.getValue();
            for (SchedulingGameActionProductProducing productProducing : actionSet) {
                if (previousIteratedAction == null) {
                    previousIteratedAction = productProducing;
                    previousIteratedActionProducingDateTime = timeSlot.getStart();

                    SchedulingProducingExecutionMode producingExecutionMode
                            = previousIteratedAction.getPlanningProducingExecutionMode();
                    previousIteratedActionCompletedDateTime
                            = previousIteratedActionProducingDateTime.plus(
                                    producingExecutionMode.getExecuteDuration()
                    );
                } else {

                }
            }
        }
    }

    @Override
    public String toString() {
        return "{\"SchedulingFactoryInstance\":{"
               + "        \"factoryInfo\":" + factoryInfo
               + ",         \"seqNum\":\"" + seqNum + "\""
               + "}}";
    }

}
