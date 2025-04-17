package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import zzk.townshipscheduler.backend.scheduling.model.ISchedulingFactoryOrFactoryArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangementFactoryTypeQueue;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
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
        process(scoreDirector, queueProducingArrangement);
    }

    private void process(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {
        backwardUpdate(scoreDirector, queueProducingArrangement);
    }

    private void backwardUpdate(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {
        SchedulingProducingArrangementFactoryTypeQueue iteratingProducingArrangement
                = queueProducingArrangement;
        SchedulingProducingArrangementFactoryTypeQueue previousIteratingProducingArrangement
                = getPreviousQueueProducingArrangement(iteratingProducingArrangement);
        LocalDateTime computedProducingDateTime
                = calcProducingDateTimeForCurrentArrangement(
                iteratingProducingArrangement, previousIteratingProducingArrangement
        );
        Integer computingSequence
                = calcSequenceForCurrentArrangement(queueProducingArrangement);

        while (
                iteratingProducingArrangement != null
                && !Objects.equals(
                        iteratingProducingArrangement.getShadowProducingDateTime(),
                        computedProducingDateTime
                )
        ) {
            doShadowVariableUpdate(
                    scoreDirector,
                    SchedulingProducingArrangementFactoryTypeQueue.class,
                    queueProducingArrangement,
                    SchedulingProducingArrangementFactoryTypeQueue.SHADOW_FACTORY_ARRANGEMENT_SEQUENCE,
                    LocalDateTime.class,
                    computedProducingDateTime,
                    SchedulingProducingArrangementFactoryTypeQueue::setShadowProducingDateTime
            );

            if (
                    !Objects.equals(
                            iteratingProducingArrangement.getShadowFactoryArrangementSequence(),
                            computingSequence
                    )
            ) {
                doShadowVariableUpdate(
                        scoreDirector,
                        SchedulingProducingArrangementFactoryTypeQueue.class,
                        queueProducingArrangement,
                        SchedulingProducingArrangementFactoryTypeQueue.SHADOW_FACTORY_ARRANGEMENT_SEQUENCE,
                        Integer.class,
                        computingSequence,
                        SchedulingProducingArrangementFactoryTypeQueue::setShadowFactoryArrangementSequence
                );
            }

            previousIteratingProducingArrangement = iteratingProducingArrangement;
            iteratingProducingArrangement = iteratingProducingArrangement.getNextQueueProducingArrangement();
            computedProducingDateTime = (computedProducingDateTime == null || iteratingProducingArrangement == null)
                    ? null
                    : calcProducingDateTimeForCurrentArrangement(
                            iteratingProducingArrangement, previousIteratingProducingArrangement
                    );
            computingSequence = iteratingProducingArrangement == null
                    ? null
                    : calcSequenceForCurrentArrangement(
                            iteratingProducingArrangement
                    );
        }
    }

    private SchedulingProducingArrangementFactoryTypeQueue getPreviousQueueProducingArrangement(
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {
        SchedulingProducingArrangementFactoryTypeQueue previousQueueProducingArrangement = null;
        ISchedulingFactoryOrFactoryArrangement planningPreviousProducingArrangementOrFactory
                = queueProducingArrangement.getPlanningPreviousProducingArrangementOrFactory();
        if (planningPreviousProducingArrangementOrFactory instanceof SchedulingProducingArrangementFactoryTypeQueue) {
            previousQueueProducingArrangement = ((SchedulingProducingArrangementFactoryTypeQueue) planningPreviousProducingArrangementOrFactory);
        } else {
            return null;
        }
        return previousQueueProducingArrangement;
    }

    private LocalDateTime calcProducingDateTimeForCurrentArrangement(
            SchedulingProducingArrangementFactoryTypeQueue currentProducingArrangement,
            @Nullable SchedulingProducingArrangementFactoryTypeQueue previousProducingArrangement
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

    private static Integer calcSequenceForCurrentArrangement(
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {
        return queueProducingArrangement
                       .getPreviousQueueProducingArrangement(queueProducingArrangement)
                       .map(SchedulingProducingArrangementFactoryTypeQueue::getShadowFactoryArrangementSequence)
                       .orElse(0) + 1;
    }

    private <E, V> void doShadowVariableUpdate(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            Class<E> entityClass,
            E entity,
            String shadowVariableName,
            Class<V> valueClass,
            V value,
            BiConsumer<E, V> entitySetFunctional
    ) {
        scoreDirector.beforeVariableChanged(
                entity,
                shadowVariableName
        );
        entitySetFunctional.accept(entity, value);
        scoreDirector.afterVariableChanged(
                entity,
                shadowVariableName
        );
    }

    private void dataTimeSlotBackwardUpdate(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {

        List<SchedulingProducingArrangementFactoryTypeQueue> flattenProducingArrangements = new ArrayList<>();
        flattenProducingArrangements.addAll(
                queueProducingArrangement.getBackwardArrangements(queueProducingArrangement)
        );
        flattenProducingArrangements.addAll(
                queueProducingArrangement.getForwardArrangements()
        );

        if (flattenProducingArrangements.isEmpty()) {
            return;
        }

        TreeMap<SchedulingDateTimeSlot, List<SchedulingProducingArrangementFactoryTypeQueue>> dateTimeSlotArrangementsMap
                = flattenProducingArrangements
                .stream()
                .collect(
                        Collectors.groupingBy(
                                SchedulingProducingArrangementFactoryTypeQueue::getPlanningDateTimeSlot,
                                TreeMap::new,
                                Collectors.toList()
                        )
                );

        dateTimeSlotArrangementsMap.forEach((dateTimeSlot, producingArrangements) -> {
            final LocalDateTime currentArrangementMandatoryDateTime = dateTimeSlot.getStart();
            LocalDateTime previousCompleteMandatoryDateTime = null;
            SchedulingProducingArrangementFactoryTypeQueue previousArrangementMandatory = null;
            SchedulingProducingArrangementFactoryTypeQueue currentArrangementMandatory = null;
            ListIterator<SchedulingProducingArrangementFactoryTypeQueue> listedIterator
                    = producingArrangements.listIterator();

            if (listedIterator.hasNext()) {
                currentArrangementMandatory = listedIterator.next();
                LocalDateTime computedProducingDateTime
                        = calcProducingDateTimeForCurrentArrangement(
                        currentArrangementMandatoryDateTime,
                        previousCompleteMandatoryDateTime
                );
                if (!Objects.equals(
                        currentArrangementMandatory.getProducingDateTime(),
                        computedProducingDateTime
                )) {
                    doShadowVariableUpdate(
                            scoreDirector,
                            SchedulingProducingArrangementFactoryTypeQueue.class,
                            queueProducingArrangement,
                            SchedulingProducingArrangementFactoryTypeQueue.SHADOW_PRODUCING_DATE_TIME,
                            LocalDateTime.class,
                            computedProducingDateTime,
                            SchedulingProducingArrangementFactoryTypeQueue::setShadowProducingDateTime
                    );
                }

                while (listedIterator.hasNext()) {
                    if (!Objects.equals(
                            currentArrangementMandatory.getProducingDateTime(),
                            computedProducingDateTime
                    )) {
                        doShadowVariableUpdate(
                                scoreDirector,
                                SchedulingProducingArrangementFactoryTypeQueue.class,
                                currentArrangementMandatory,
                                SchedulingProducingArrangementFactoryTypeQueue.SHADOW_PRODUCING_DATE_TIME,
                                LocalDateTime.class,
                                computedProducingDateTime,
                                SchedulingProducingArrangementFactoryTypeQueue::setShadowProducingDateTime
                        );
                    }

                    currentArrangementMandatory = listedIterator.next();
                    previousArrangementMandatory = currentArrangementMandatory;
                    previousCompleteMandatoryDateTime = previousArrangementMandatory.getCompletedDateTime();
                    computedProducingDateTime
                            = calcProducingDateTimeForCurrentArrangement(
                            currentArrangementMandatoryDateTime,
                            previousCompleteMandatoryDateTime
                    );
                }
            }

        });

    }

    private LocalDateTime calcProducingDateTimeForCurrentArrangement(
            LocalDateTime currentArrangementArrangeDateTime,
            LocalDateTime previousArrangementCompletedDateTime
    ) {
        LocalDateTime computedProducingDateTime = null;

        if (previousArrangementCompletedDateTime == null) {
            computedProducingDateTime = currentArrangementArrangeDateTime;
        } else {
            computedProducingDateTime
                    = previousArrangementCompletedDateTime.isBefore(currentArrangementArrangeDateTime)
                    ? currentArrangementArrangeDateTime
                    : previousArrangementCompletedDateTime;
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
        process(scoreDirector, queueProducingArrangement);
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
        process(scoreDirector, queueProducingArrangement);
    }


}
