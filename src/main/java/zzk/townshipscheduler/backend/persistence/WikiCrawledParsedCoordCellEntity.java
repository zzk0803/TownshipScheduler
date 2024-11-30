package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import zzk.townshipscheduler.backend.crawling.CrawledDataCell;
import zzk.townshipscheduler.backend.crawling.CrawledDataCoordinate;

import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
public class WikiCrawledParsedCoordCellEntity {

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

    public WikiCrawledParsedCoordCellEntity(CrawledDataCoordinate coord, CrawledDataCell cell) {
        this.tableZone = coord.getTableZone();
        this.numberAtArticleTable = coord.getTable();
        this.rowInCoord = coord.getRow();
        this.columnInCoord = coord.getColumn();
        this.html = cell.getHtml();
        this.text = cell.getText();
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object) return true;
        if (object == null) return false;
        Class<?> oEffectiveClass = object instanceof HibernateProxy
                ? ((HibernateProxy) object).getHibernateLazyInitializer().getPersistentClass()
                : object.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        WikiCrawledParsedCoordCellEntity that = (WikiCrawledParsedCoordCellEntity) object;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                .hashCode() : getClass().hashCode();
    }

}
