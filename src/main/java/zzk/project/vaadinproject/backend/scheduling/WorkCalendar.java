package zzk.project.vaadinproject.backend.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class WorkCalendar {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate; // Inclusive

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate; // Exclusive

    public WorkCalendar() {
    }

    public WorkCalendar(LocalDate fromDate, LocalDate toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    @Override
    public String toString() {
        return fromDate + " - " + toDate;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

}
