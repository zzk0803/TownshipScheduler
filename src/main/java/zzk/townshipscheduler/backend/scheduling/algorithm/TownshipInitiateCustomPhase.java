package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class TownshipInitiateCustomPhase implements PhaseCommand<TownshipSchedulingProblem> {

    @Override
    public void changeWorkingSolution(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            BooleanSupplier isPhaseTerminated
    ) {
        if (isPhaseTerminated.getAsBoolean()) {
            return;
        }

        TownshipSchedulingProblem workingSolution = scoreDirector.getWorkingSolution();
        List<SchedulingDateTimeSlot> dateTimeSlotSet = workingSolution.getSchedulingDateTimeSlots();
        List<BaseSchedulingProducingArrangement> producingArrangements = workingSolution.getBaseProducingArrangements();
        List<SchedulingFactoryInstanceTypeSlot> slotFactoryInstanceList = workingSolution.getSchedulingFactoryInstanceTypeSlotList();
        List<SchedulingFactoryInstanceTypeQueue> queueFactoryInstanceList = workingSolution.getSchedulingFactoryInstanceTypeQueueList();
        LocalDateTime startDateTime = workingSolution.getSchedulingWorkTimeLimit().getStartDateTime();

        Map<SchedulingFactoryInstanceTypeQueue, SchedulingProducingArrangementFactoryTypeQueue> factoryProducingMap = new LinkedHashMap<>();
        for (BaseSchedulingProducingArrangement producingArrangement : producingArrangements) {

            if (producingArrangement instanceof SchedulingProducingArrangementFactoryTypeSlot slotProducingArrangement) {
                Duration approximateDelay
                        = producingArrangement.getDeepPrerequisiteProducingArrangements()
                        .stream()
                        .map(BaseSchedulingProducingArrangement::getProducingDuration)
                        .max(Duration::compareTo)
                        .orElse(Duration.ZERO);
                SchedulingDateTimeSlot computedDataTimeSlot
                        = SchedulingDateTimeSlot.fromRangeCeil(
                        dateTimeSlotSet,
                        startDateTime.plus(approximateDelay)
                ).orElse(dateTimeSlotSet.getLast());

                scoreDirector.beforeVariableChanged(
                        slotProducingArrangement,
                        BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                producingArrangement.setPlanningDateTimeSlot(computedDataTimeSlot);
                scoreDirector.afterVariableChanged(
                        slotProducingArrangement,
                        BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                scoreDirector.triggerVariableListeners();

                SchedulingFactoryInfo requireFactory
                        = slotProducingArrangement.getSchedulingProduct().getRequireFactory();
                for (SchedulingFactoryInstanceTypeSlot slotFactoryInstance : slotFactoryInstanceList) {
                    if (requireFactory.typeEqual(slotFactoryInstance.getSchedulingFactoryInfo())) {
                        scoreDirector.beforeVariableChanged(
                                slotProducingArrangement,
                                SchedulingProducingArrangementFactoryTypeSlot.PLANNING_FACTORY
                        );
                        slotProducingArrangement.setPlanningFactory(slotFactoryInstance);
                        scoreDirector.afterVariableChanged(
                                slotProducingArrangement,
                                SchedulingProducingArrangementFactoryTypeSlot.PLANNING_FACTORY
                        );
                        scoreDirector.triggerVariableListeners();
                        break;
                    }
                }
            } else if (producingArrangement instanceof SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement) {
                Duration approximateDelay
                        = producingArrangement.getPrerequisiteProducingArrangements()
                        .stream()
                        .map(BaseSchedulingProducingArrangement::getProducingDuration)
                        .max(Duration::compareTo)
                        .orElse(Duration.ZERO);
                SchedulingDateTimeSlot computedDataTimeSlot
                        = SchedulingDateTimeSlot.fromRangeCeil(
                        dateTimeSlotSet,
                        startDateTime.plus(approximateDelay)
                ).orElse(dateTimeSlotSet.getLast());

                scoreDirector.beforeVariableChanged(
                        queueProducingArrangement,
                        BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                queueProducingArrangement.setPlanningDateTimeSlot(computedDataTimeSlot);
                scoreDirector.afterVariableChanged(
                        queueProducingArrangement,
                        BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                scoreDirector.triggerVariableListeners();

                SchedulingFactoryInfo requireFactory
                        = queueProducingArrangement.getSchedulingProduct().getRequireFactory();
                for (SchedulingFactoryInstanceTypeQueue queueFactoryInstance : queueFactoryInstanceList) {
                    if (queueFactoryInstance.getSchedulingFactoryInfo() == requireFactory) {
                        SchedulingProducingArrangementFactoryTypeQueue mayNullFactoryToProducing
                                = factoryProducingMap.get(queueFactoryInstance);
                        if (mayNullFactoryToProducing != null) {
                            scoreDirector.beforeVariableChanged(
                                    queueProducingArrangement,
                                    SchedulingProducingArrangementFactoryTypeQueue.PLANNING_PREVIOUS
                            );
                            queueProducingArrangement.setPlanningPreviousProducingArrangementOrFactory(
                                    mayNullFactoryToProducing
                            );
                            scoreDirector.afterVariableChanged(
                                    queueProducingArrangement,
                                    SchedulingProducingArrangementFactoryTypeQueue.PLANNING_PREVIOUS
                            );
                            scoreDirector.triggerVariableListeners();
                            factoryProducingMap.put(queueFactoryInstance, queueProducingArrangement);
                        } else {
                            scoreDirector.beforeVariableChanged(
                                    queueProducingArrangement,
                                    SchedulingProducingArrangementFactoryTypeQueue.PLANNING_PREVIOUS
                            );
                            queueProducingArrangement.setPlanningPreviousProducingArrangementOrFactory(
                                    queueFactoryInstance
                            );
                            scoreDirector.afterVariableChanged(
                                    queueProducingArrangement,
                                    SchedulingProducingArrangementFactoryTypeQueue.PLANNING_PREVIOUS
                            );
                            scoreDirector.triggerVariableListeners();
                            factoryProducingMap.put(queueFactoryInstance, queueProducingArrangement);
                        }
                        break;
                    }
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

}
