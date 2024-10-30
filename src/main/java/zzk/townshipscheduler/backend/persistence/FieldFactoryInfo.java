package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldFactoryInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;

    @OneToMany
    private List<Goods> products;

    private Integer maxInstanceCapacity;

    private Integer defaultQueueCapacity;

    private Integer maxQueueCapacity;

    private Integer defaultWindowCapacity;

    private Integer maxWindowCapacity;


}
