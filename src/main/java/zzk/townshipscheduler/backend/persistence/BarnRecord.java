package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
public class BarnRecord implements Comparable<BarnRecord> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    private LocalDateTime dateTime;

    @Getter
    @Column(name = "good_id")
    private Long goodId;

    @Getter
    @Enumerated(EnumType.STRING)
    private BarnAction barnAction;

    public BarnRecord() {
    }

    public BarnRecord(Long goodId, BarnAction barnAction) {
        this(LocalDateTime.now(), goodId, barnAction);
    }

    public BarnRecord(LocalDateTime dateTime, Long goodId, BarnAction barnAction) {
        this.dateTime = dateTime;
        this.goodId = goodId;
        this.barnAction = barnAction;
    }

    @Override
    public int compareTo(BarnRecord that) {
        return this.dateTime.compareTo(that.dateTime);
    }

    public enum BarnAction {
        SAVE,
        TAKE
    }

}
