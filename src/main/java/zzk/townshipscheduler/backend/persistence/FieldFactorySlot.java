package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class FieldFactorySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private FieldFactoryInfo fieldFactoryInfo;

    @OneToMany
    private List<FieldFactoryRecord> factoryRecords;

}
