package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import zzk.townshipscheduler.backend.scheduling.model.*;

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
        List<BaseProducingArrangement> producingArrangements = workingSolution.getBaseProducingArrangements();
        List<SchedulingTypeSlotFactoryInstance> slotFactoryInstanceList = workingSolution.getSchedulingTypeSlotFactoryInstanceList();
        List<SchedulingTypeQueueFactoryInstance> queueFactoryInstanceList = workingSolution.getSchedulingTypeQueueFactoryInstanceList();

        Map<SchedulingTypeQueueFactoryInstance, SchedulingFactoryQueueProducingArrangement> factoryProducingMap = new LinkedHashMap<>();
        for (BaseProducingArrangement producingArrangement : producingArrangements) {
            if (producingArrangement instanceof SchedulingFactorySlotProducingArrangement slotProducingArrangement) {
                scoreDirector.beforeVariableChanged(
                        slotProducingArrangement,
                        BaseProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                slotProducingArrangement.setPlanningDateTimeSlot(dateTimeSlotSet.getFirst());
                scoreDirector.afterVariableChanged(
                        slotProducingArrangement,
                        BaseProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                scoreDirector.triggerVariableListeners();

                SchedulingFactoryInfo requireFactory
                        = slotProducingArrangement.getSchedulingProduct().getRequireFactory();
                for (SchedulingTypeSlotFactoryInstance slotFactoryInstance : slotFactoryInstanceList) {
                    if (slotFactoryInstance.getSchedulingFactoryInfo() == requireFactory) {
                        scoreDirector.beforeVariableChanged(
                                slotProducingArrangement,
                                SchedulingFactorySlotProducingArrangement.PLANNING_FACTORY
                        );
                        slotProducingArrangement.setPlanningFactory(slotFactoryInstance);
                        scoreDirector.afterVariableChanged(
                                slotProducingArrangement,
                                SchedulingFactorySlotProducingArrangement.PLANNING_FACTORY
                        );
                        scoreDirector.triggerVariableListeners();
                        break;
                    }
                }
            } else if (producingArrangement instanceof SchedulingFactoryQueueProducingArrangement queueProducingArrangement) {
                scoreDirector.beforeVariableChanged(
                        queueProducingArrangement,
                        BaseProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                queueProducingArrangement.setPlanningDateTimeSlot(dateTimeSlotSet.getFirst());
                scoreDirector.afterVariableChanged(
                        queueProducingArrangement,
                        BaseProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                scoreDirector.triggerVariableListeners();

                SchedulingFactoryInfo requireFactory
                        = queueProducingArrangement.getSchedulingProduct().getRequireFactory();
                for (SchedulingTypeQueueFactoryInstance queueFactoryInstance : queueFactoryInstanceList) {
                    if (queueFactoryInstance.getSchedulingFactoryInfo() == requireFactory) {
                        SchedulingFactoryQueueProducingArrangement mayNullFactoryToProducing
                                = factoryProducingMap.get(queueFactoryInstance);
                        if (mayNullFactoryToProducing != null) {
                            scoreDirector.beforeVariableChanged(
                                    queueProducingArrangement,
                                    SchedulingFactoryQueueProducingArrangement.PLANNING_PREVIOUS
                            );
                            queueProducingArrangement.setPlanningPreviousProducingArrangementOrFactory(
                                    mayNullFactoryToProducing
                            );
                            scoreDirector.afterVariableChanged(
                                    queueProducingArrangement,
                                    SchedulingFactoryQueueProducingArrangement.PLANNING_PREVIOUS
                            );
                            scoreDirector.triggerVariableListeners();
                            factoryProducingMap.put(queueFactoryInstance, queueProducingArrangement);
                        } else {
                            scoreDirector.beforeVariableChanged(
                                    queueProducingArrangement,
                                    SchedulingFactoryQueueProducingArrangement.PLANNING_PREVIOUS
                            );
                            queueProducingArrangement.setPlanningPreviousProducingArrangementOrFactory(
                                    queueFactoryInstance
                            );
                            scoreDirector.afterVariableChanged(
                                    queueProducingArrangement,
                                    SchedulingFactoryQueueProducingArrangement.PLANNING_PREVIOUS
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
