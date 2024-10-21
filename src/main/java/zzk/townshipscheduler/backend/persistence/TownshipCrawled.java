package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "township_crawled")
public class TownshipCrawled {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdDateTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private Type type = Type.IMAGE;

    private String text = "";

    @Lob
    private String html = "";

    @Lob
    private byte[] imageBytes;

    public enum Type {
        HTML,
        IMAGE
    }

}
