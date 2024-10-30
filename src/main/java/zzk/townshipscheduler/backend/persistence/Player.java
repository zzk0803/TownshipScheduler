package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    private Integer level;

    @OneToOne
    private Barn barn;

    @OneToMany
    private List<FieldFactorySlot> factorySlotList;

}
