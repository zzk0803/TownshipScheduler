package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.solver.event.NewBestSolutionEvent;

import java.util.function.Consumer;

public interface BestSolutionEventConsumerAdapter<Solution_> {

    static <Solution_> Consumer<NewBestSolutionEvent<Solution_>> of(Consumer<Solution_> problemConsumer) {
        return solutionNewBestSolutionEvent -> {
            Solution_ solution = solutionNewBestSolutionEvent.solution();
            problemConsumer.accept(solution);
        };
    }

}
