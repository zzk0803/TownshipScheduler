package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@DynamicUpdate
public class Barn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Player player;

    private LocalDateTime lastStockCheck;

    private Long stockCheckCount;

    @OneToMany
    private List<BarnRecord> barnRecords = new ArrayList<>();

}
