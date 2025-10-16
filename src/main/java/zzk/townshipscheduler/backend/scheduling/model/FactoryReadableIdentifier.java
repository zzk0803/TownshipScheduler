package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.stream.IntStream;

@Getter
@EqualsAndHashCode
public final class FactoryReadableIdentifier implements CharSequence, Comparable<FactoryReadableIdentifier> {

    private final int factoryId;

    private final int factoryLevel;

    private final String factoryCategory;

    private final int seqNum;

    @JsonCreator
    public FactoryReadableIdentifier(SchedulingFactoryInstance schedulingFactoryInstance) {
        this.factoryId = schedulingFactoryInstance.getId();
        this.factoryLevel = schedulingFactoryInstance.getSchedulingFactoryInfo().getLevel();
        this.factoryCategory = schedulingFactoryInstance.getCategoryName();
        this.seqNum = schedulingFactoryInstance.getSeqNum();
    }

    @JsonCreator
    public FactoryReadableIdentifier(int factoryId, int factoryLevel, String factoryCategory, int seqNum) {
        this.factoryId = factoryId;
        this.factoryLevel = factoryLevel;
        this.factoryCategory = factoryCategory;
        this.seqNum = seqNum;
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public String toString() {
        return factoryCategory + "#" + seqNum;
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public boolean isEmpty() {
        return toString().isEmpty();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    public IntStream chars() {
        return toString().chars();
    }

    @Override
    public IntStream codePoints() {
        return toString().codePoints();
    }

    @Override
    public int compareTo(FactoryReadableIdentifier that) {
        return Comparator.comparing(FactoryReadableIdentifier::getFactoryLevel)
                .thenComparingInt(FactoryReadableIdentifier::getFactoryId)
                .thenComparingInt(FactoryReadableIdentifier::getSeqNum)
                .compare(this, that);
    }

}
