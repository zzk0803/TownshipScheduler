package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SchedulingProducingArrangementFactoryTypeQueueDateTimeVariableListener
        implements VariableListener<TownshipSchedulingProblem, SchedulingProducingArrangementFactoryTypeQueue> {

    @Override
    public void beforeVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {

    }

    @Override
    public void afterVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {
        doUpdate(scoreDirector, queueProducingArrangement);
    }

    private void doUpdate(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {
        SchedulingFactoryInstanceTypeQueue planningFactory
                = queueProducingArrangement.getPlanningFactoryInstance();

        SchedulingProducingArrangementFactoryTypeQueue previousQueueProducingArrangement = null;
        ISchedulingFactoryOrFactoryArrangement planningPreviousProducingArrangementOrFactory
                = queueProducingArrangement.getPlanningPreviousProducingArrangementOrFactory();
        if (planningPreviousProducingArrangementOrFactory instanceof SchedulingProducingArrangementFactoryTypeQueue) {
            previousQueueProducingArrangement = ((SchedulingProducingArrangementFactoryTypeQueue) planningPreviousProducingArrangementOrFactory);
        }

        LocalDateTime computedProducingDateTime
                = calcProducingDateTime(
                previousQueueProducingArrangement,
                queueProducingArrangement
        );

        SchedulingProducingArrangementFactoryTypeQueue previousIteratingProducingArrangement = null;
        while (
                queueProducingArrangement != null
                && !Objects.equals(
                        queueProducingArrangement.getShadowProducingDateTime(),
                        computedProducingDateTime
                )
        ) {
            scoreDirector.beforeVariableChanged(
                    queueProducingArrangement,
                    SchedulingProducingArrangementFactoryTypeQueue.SHADOW_PRODUCING_DATE_TIME
            );
            queueProducingArrangement.setShadowProducingDateTime(computedProducingDateTime);
            scoreDirector.afterVariableChanged(
                    queueProducingArrangement,
                    SchedulingProducingArrangementFactoryTypeQueue.SHADOW_PRODUCING_DATE_TIME
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
            @Nullable SchedulingProducingArrangementFactoryTypeQueue previousProducingArrangement,
            SchedulingProducingArrangementFactoryTypeQueue currentProducingArrangement
    ) {
        LocalDateTime computedProducingDateTime = null;
        LocalDateTime previousCompletedDateTime =
                previousProducingArrangement == null
                        ? null
                        : previousProducingArrangement.getCompletedDateTime();

        LocalDateTime planningDateTime = currentProducingArrangement.getArrangeDateTime();
        if (planningDateTime == null) {
            return null;
        }

        if (previousCompletedDateTime == null) {
            computedProducingDateTime = planningDateTime;
        } else {
            computedProducingDateTime
                    = previousCompletedDateTime.isBefore(planningDateTime)
                    ? planningDateTime
                    : previousCompletedDateTime;
        }
        return computedProducingDateTime;
    }

    @Override
    public void beforeEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {

    }

    @Override
    public void afterEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {
        doUpdate(scoreDirector, queueProducingArrangement);
    }

    @Override
    public void beforeEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {

    }

    @Override
    public void afterEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {
        doUpdate(scoreDirector, queueProducingArrangement);
    }


}
