package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.stream.IntStream;

public record FactoryReadableIdentifier(
        @EqualsAndHashCode.Include long categoryId,
        String categoryName,
        @EqualsAndHashCode.Include int seqNum
)
        implements CharSequence, Comparable<FactoryReadableIdentifier>, Serializable {

    @Serial
    private static final long serialVersionUID = -3940474169751457218L;

    @JsonCreator
    public FactoryReadableIdentifier {
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public String toString() {
        return categoryName + "#" + seqNum;
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
        return Comparator.comparingLong(FactoryReadableIdentifier::categoryId)
                .thenComparingInt(FactoryReadableIdentifier::seqNum)
                .compare(this, that);
    }

}
