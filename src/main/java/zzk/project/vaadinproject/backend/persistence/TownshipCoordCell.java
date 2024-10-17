package zzk.project.vaadinproject.backend.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.proxy.HibernateProxy;
import zzk.project.vaadinproject.backend.crawling.RawDataCrawledCell;
import zzk.project.vaadinproject.backend.crawling.RawDataCrawledCoord;

import java.util.Objects;

@Data
@NoArgsConstructor
@Entity
@Table(name = "township_coord_cell")
public class TownshipCoordCell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tableZone;

    private int numberAtArticleTable;

    private int rowInCoord;

    private int columnInCoord;

    @Lob
    private String html = "";

    private String text = "";

    public TownshipCoordCell(RawDataCrawledCoord coord, RawDataCrawledCell cell) {
        this.tableZone = coord.getTableZone();
        this.numberAtArticleTable = coord.getTable();
        this.rowInCoord = coord.getRow();
        this.columnInCoord = coord.getColumn();
        this.html = cell.getHtml();
        this.text = cell.getText();
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        TownshipCoordCell that = (TownshipCoordCell) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

}
