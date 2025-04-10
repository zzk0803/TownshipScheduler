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
        List<BaseSchedulingProducingArrangement> producingArrangements = workingSolution.getBaseProducingArrangements();
        List<SchedulingFactoryInstanceTypeSlot> slotFactoryInstanceList = workingSolution.getSchedulingFactoryInstanceTypeSlotList();
        List<SchedulingFactoryInstanceTypeQueue> queueFactoryInstanceList = workingSolution.getSchedulingFactoryInstanceTypeQueueList();

        Map<SchedulingFactoryInstanceTypeQueue, SchedulingProducingArrangementFactoryTypeQueue> factoryProducingMap = new LinkedHashMap<>();
        for (BaseSchedulingProducingArrangement producingArrangement : producingArrangements) {
            if (producingArrangement instanceof SchedulingProducingArrangementFactoryTypeSlot slotProducingArrangement) {
                scoreDirector.beforeVariableChanged(
                        slotProducingArrangement,
                        BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                slotProducingArrangement.setPlanningDateTimeSlot(dateTimeSlotSet.getFirst());
                scoreDirector.afterVariableChanged(
                        slotProducingArrangement,
                        BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                scoreDirector.triggerVariableListeners();

                SchedulingFactoryInfo requireFactory
                        = slotProducingArrangement.getSchedulingProduct().getRequireFactory();
                for (SchedulingFactoryInstanceTypeSlot slotFactoryInstance : slotFactoryInstanceList) {
                    if (slotFactoryInstance.getSchedulingFactoryInfo() == requireFactory) {
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
                scoreDirector.beforeVariableChanged(
                        queueProducingArrangement,
                        BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
                );
                queueProducingArrangement.setPlanningDateTimeSlot(dateTimeSlotSet.getFirst());
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
