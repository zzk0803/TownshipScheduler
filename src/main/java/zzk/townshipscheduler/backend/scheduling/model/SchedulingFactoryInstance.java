package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SchedulingFactoryInstance {

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    @JsonIgnore
    @EqualsAndHashCode.Include
    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int parallelProducing = 1;

    @ToString.Include
    private int producingQueue;

    @ToString.Include
    private int reapWindowSize;

    @Setter(AccessLevel.PRIVATE)
    @ToString.Include
    private FactoryReadableIdentifier factoryReadableIdentifier;

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(
                new FactoryReadableIdentifier(getCategoryName(), getSeqNum())
        );
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return ProducingStructureType.QUEUE.equals(getSchedulingFactoryInfo().getProducingStructureType());
    }

    public boolean weatherFactoryProducingTypeIsSlot() {
        return ProducingStructureType.SLOT.equals(getSchedulingFactoryInfo().getProducingStructureType());
    }

    @Override
    public String toString() {
        return "SchedulingFactoryInstance{" +
               "readableIdentifier='" + factoryReadableIdentifier + '\'' +
               ", producingLength=" + producingQueue +
               ", reapWindowSize=" + reapWindowSize +
               '}';
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo().typeEqual(that.getSchedulingFactoryInfo());
    }

}
