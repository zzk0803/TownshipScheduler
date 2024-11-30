package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class SchedulingGamePlayer {

    private String playerName;

    private int playerLevel;

    @JsonIgnore
    private List<SchedulingProducing> schedulingProducingList;

}
