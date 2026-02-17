package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Getter
public class FactoryDateTimeReadableIdentifier extends FactoryReadableIdentifier {

    public static final Comparator<FactoryDateTimeReadableIdentifier> COMPARATOR = Comparator.comparing(
            FactoryDateTimeReadableIdentifier::getStart);

    private final LocalDateTime start;

    private final LocalDateTime end;

    public FactoryDateTimeReadableIdentifier(
            String factoryCategory,
            int seqNum,
            LocalDateTime start,
            LocalDateTime end
    ) {
        super(factoryCategory, seqNum);
        this.start = start;
        this.end = end;
    }

    public FactoryDateTimeReadableIdentifier(
            SchedulingFactoryInstance schedulingFactoryInstance, LocalDateTime start,
            LocalDateTime end
    ) {
        super(
                schedulingFactoryInstance.getFactoryReadableIdentifier().getFactoryCategory(),
                schedulingFactoryInstance.getFactoryReadableIdentifier().getSeqNum()
        );
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return super.toString() + "(%s~%s)".formatted(
                start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    @Override
    public int compareTo(FactoryReadableIdentifier factoryReadableIdentifier) {
        if (factoryReadableIdentifier instanceof FactoryDateTimeReadableIdentifier that) {
            return COMPARATOR.compare(this, that);
        }
        return super.compareTo(factoryReadableIdentifier);
    }

}
