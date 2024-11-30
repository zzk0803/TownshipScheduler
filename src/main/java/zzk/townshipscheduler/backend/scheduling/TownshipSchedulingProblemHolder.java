package zzk.townshipscheduler.backend.scheduling;

import org.springframework.stereotype.Component;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class TownshipSchedulingProblemHolder {

    private AtomicReference<TownshipSchedulingProblem> problemAtomicReference = new AtomicReference<>();

    public TownshipSchedulingProblem read() {
        return problemAtomicReference.get();
    }

    public void write(TownshipSchedulingProblem townshipSchedulingProblem) {
        problemAtomicReference.set(townshipSchedulingProblem);
    }

}
