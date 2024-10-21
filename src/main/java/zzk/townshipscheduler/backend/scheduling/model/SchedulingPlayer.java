package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;

import java.util.Map;

@Data
public class SchedulingPlayer {

    private Long id;

    private String name;

    private Integer level;

    private Warehouse warehouse;

    private Map<String, Integer> producingAbilitySlotAmountMap;


}
