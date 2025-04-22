package zzk.townshipscheduler.backend.scheduling.model;

import lombok.EqualsAndHashCode;

import java.util.Comparator;
import java.util.stream.IntStream;

@EqualsAndHashCode
public final class FactoryReadableIdentifier implements CharSequence, Comparable<FactoryReadableIdentifier> {

    private final String factoryCategory;

    private final int seqNum;

    public FactoryReadableIdentifier(String factoryCategory, int seqNum) {
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
        return Comparator.comparing(FactoryReadableIdentifier::getFactoryCategory)
                .thenComparingInt(FactoryReadableIdentifier::getSeqNum)
                .compare(this, that);
    }

    public String getFactoryCategory() {
        return factoryCategory;
    }

    public int getSeqNum() {
        return seqNum;
    }

}
