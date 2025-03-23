package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import zzk.townshipscheduler.backend.scheduling.model.ISchedulingFactoryOrFactoryArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryQueueProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingTypeQueueFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.LocalDateTime;
import java.util.Objects;

public class ProducingArrangementFactorySequenceVariableListener
        implements VariableListener<TownshipSchedulingProblem, SchedulingFactoryQueueProducingArrangement> {

    @Override
    public void beforeVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingFactoryQueueProducingArrangement queueProducingArrangement
    ) {

    }

    @Override
    public void afterVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingFactoryQueueProducingArrangement queueProducingArrangement
    ) {
        doUpdate(scoreDirector, queueProducingArrangement);
    }

    private void doUpdate(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingFactoryQueueProducingArrangement queueProducingArrangement
    ) {
        SchedulingTypeQueueFactoryInstance planningFactory
                = queueProducingArrangement.getPlanningFactoryInstance();
        SchedulingFactoryQueueProducingArrangement previousQueueProducingArrangement = null;

        ISchedulingFactoryOrFactoryArrangement planningPreviousFactorySequenceOrFactory
                = queueProducingArrangement.getPlanningPreviousFactorySequenceOrFactory();
        if (planningPreviousFactorySequenceOrFactory instanceof SchedulingFactoryQueueProducingArrangement) {
            previousQueueProducingArrangement = ((SchedulingFactoryQueueProducingArrangement) planningPreviousFactorySequenceOrFactory);
        }

        LocalDateTime computedProducingDateTime
                = calcProducingDateTime(
                previousQueueProducingArrangement,
                queueProducingArrangement
        );

        SchedulingFactoryQueueProducingArrangement previousIteratingProducingArrangement = null;
        while (
                queueProducingArrangement != null
                && !Objects.equals(
                        queueProducingArrangement.getShadowProducingDateTime(),
                        computedProducingDateTime
                )
        ) {
            scoreDirector.beforeVariableChanged(
                    queueProducingArrangement,
                    SchedulingFactoryQueueProducingArrangement.SHADOW_PRODUCING_DATE_TIME
            );
            queueProducingArrangement.setShadowProducingDateTime(computedProducingDateTime);
            scoreDirector.afterVariableChanged(
                    queueProducingArrangement,
                    SchedulingFactoryQueueProducingArrangement.SHADOW_PRODUCING_DATE_TIME
            );

            previousIteratingProducingArrangement = queueProducingArrangement;
            queueProducingArrangement = queueProducingArrangement.getNextQueueProducingArrangement();
            computedProducingDateTime = (computedProducingDateTime == null || queueProducingArrangement == null)
                    ? null
                    : calcProducingDateTime(
                            previousIteratingProducingArrangement,
                            queueProducingArrangement
                    );
        }
    }

    private LocalDateTime calcProducingDateTime(
            @Nullable SchedulingFactoryQueueProducingArrangement previousProducingArrangement,
            SchedulingFactoryQueueProducingArrangement currentProducingArrangement
    ) {
        LocalDateTime computedProducingDateTime = null;
        LocalDateTime previousCompletedDateTime =
                previousProducingArrangement == null
                        ? null
                        : previousProducingArrangement.getCompletedDateTime();

        LocalDateTime planningDateTime = currentProducingArrangement.getPlanningDateTimeSlotStartAsLocalDateTime();
        if (planningDateTime == null) {
            return null;
        }

        if (previousCompletedDateTime == null) {
            computedProducingDateTime = planningDateTime;
        } else {
            computedProducingDateTime = previousCompletedDateTime.isBefore(planningDateTime)
                    ? planningDateTime
                    : previousCompletedDateTime;
        }
        return computedProducingDateTime;
    }

    @Override
    public void beforeEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingFactoryQueueProducingArrangement queueProducingArrangement
    ) {

    }

    @Override
    public void afterEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingFactoryQueueProducingArrangement queueProducingArrangement
    ) {
        doUpdate(scoreDirector, queueProducingArrangement);
    }

    @Override
    public void beforeEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingFactoryQueueProducingArrangement queueProducingArrangement
    ) {

    }

    @Override
    public void afterEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingFactoryQueueProducingArrangement queueProducingArrangement
    ) {
        doUpdate(scoreDirector, queueProducingArrangement);
    }


}
