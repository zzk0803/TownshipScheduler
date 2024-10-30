package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
public class FieldFactoryRecord implements Comparable<FieldFactoryRecord> {

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
    private FactoryAction factoryAction;

    public FieldFactoryRecord() {
    }

    public FieldFactoryRecord(Long goodId, FactoryAction factoryAction) {
        this.dateTime = LocalDateTime.now();
        this.goodId = goodId;
        this.factoryAction = factoryAction;
    }

    public FieldFactoryRecord(Long id, LocalDateTime dateTime, Long goodId, FactoryAction factoryAction) {
        this.id = id;
        this.dateTime = dateTime;
        this.goodId = goodId;
        this.factoryAction = factoryAction;
    }

    @Override
    public int compareTo(FieldFactoryRecord that) {
        return this.dateTime.compareTo(that.dateTime);
    }

    public enum FactoryAction {
        ARRANGE,
        REAP
    }

}
