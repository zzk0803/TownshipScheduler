package zzk.townshipscheduler.backend.crawling;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class RawDataCrawledCoord
        implements Comparable<RawDataCrawledCoord>, Cloneable {

    public static final Comparator<RawDataCrawledCoord> COMPARATOR =
            Comparator.comparingInt(RawDataCrawledCoord::getTable)
                    .thenComparingInt(RawDataCrawledCoord::getRow)
                    .thenComparingInt(RawDataCrawledCoord::getColumn);

    public static final int BOUNDARY = 1;

    private String tableZone;

    private int table = 0;

    private int row = BOUNDARY;

    private int column = BOUNDARY;

    @Override
    public int compareTo(RawDataCrawledCoord that) {
        return COMPARATOR.compare(this, that);
    }

    public RawDataCrawledCoord cloneAndNextRow() {
        RawDataCrawledCoord clone = clone();
        clone.setRow(this.getRow() + 1);
        return clone;

    }

    @Override
    public RawDataCrawledCoord clone() {
        try {
            return (RawDataCrawledCoord) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public RawDataCrawledCoord cloneAndPreviousRow() {
        RawDataCrawledCoord clone = clone();
        clone.setRow(this.getRow() - 1);
        return clone;

    }

    public RawDataCrawledCoord cloneAndNextColumn() {
        RawDataCrawledCoord clone = clone();
        clone.setColumn(this.getColumn() + 1);
        return clone;

    }

    public RawDataCrawledCoord cloneAndPreviousColumn() {
        RawDataCrawledCoord clone = clone();
        clone.setColumn(this.getColumn() - 1);
        return clone;

    }

    public RawDataCrawledCoord cloneAndResetColumn() {
        RawDataCrawledCoord clone = clone();
        clone.setColumn(0);
        return clone;

    }

    public RawDataCrawledCoord cloneAndResetRow() {
        RawDataCrawledCoord clone = clone();
        clone.setRow(0);
        return clone;
    }

}
