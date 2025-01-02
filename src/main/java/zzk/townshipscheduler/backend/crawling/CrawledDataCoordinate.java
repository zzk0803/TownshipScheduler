package zzk.townshipscheduler.backend.crawling;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class CrawledDataCoordinate
        implements Comparable<CrawledDataCoordinate>, Cloneable {

    public static final Comparator<CrawledDataCoordinate> COMPARATOR =
            Comparator.comparingInt(CrawledDataCoordinate::getTable)
                    .thenComparingInt(CrawledDataCoordinate::getRow)
                    .thenComparingInt(CrawledDataCoordinate::getColumn);

    public static final int BOUNDARY = 1;

    private String tableZone;

    private int table = 0;

    private int row = BOUNDARY;

    private int column = BOUNDARY;

    public static CrawledDataCoordinate create() {
        return new CrawledDataCoordinate();
    }

    @Override
    public int compareTo(CrawledDataCoordinate that) {
        return COMPARATOR.compare(this, that);
    }

    public CrawledDataCoordinate cloneAndNextRow() {
        CrawledDataCoordinate clone = clone();
        clone.setRow(this.getRow() + 1);
        return clone;

    }

    @Override
    public CrawledDataCoordinate clone() {
        try {
            return (CrawledDataCoordinate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public CrawledDataCoordinate cloneAndPreviousRow() {
        CrawledDataCoordinate clone = clone();
        clone.setRow(this.getRow() - 1);
        return clone;

    }

    public CrawledDataCoordinate cloneAndNextColumn() {
        CrawledDataCoordinate clone = clone();
        clone.setColumn(this.getColumn() + 1);
        return clone;

    }

    public CrawledDataCoordinate cloneAndPreviousColumn() {
        CrawledDataCoordinate clone = clone();
        clone.setColumn(this.getColumn() - 1);
        return clone;

    }

    public CrawledDataCoordinate cloneAndResetColumn() {
        CrawledDataCoordinate clone = clone();
        clone.setColumn(0);
        return clone;

    }

    public CrawledDataCoordinate cloneAndResetRow() {
        CrawledDataCoordinate clone = clone();
        clone.setRow(0);
        return clone;
    }

}
