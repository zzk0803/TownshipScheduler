package zzk.project.vaadinproject.backend.scheduling;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class PackagingScheduleHolder {

    private final AtomicReference<PackagingSchedule> solutionReference = new AtomicReference<>();

    public PackagingSchedule read() {
        return solutionReference.get();
    }

    public void write(PackagingSchedule schedule) {
        solutionReference.set(schedule);
    }

}
