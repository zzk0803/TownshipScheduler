package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance
        implements Serializable {

    @Serial
    private static final long serialVersionUID = -7146926432206516227L;

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    private Long fieldFactoryId;

    @JsonIgnore
    @EqualsAndHashCode.Include
    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    @Setter(AccessLevel.PRIVATE)
    private FactoryReadableIdentifier factoryReadableIdentifier;

    @JsonIgnore
    @InverseRelationShadowVariable(sourceVariableName = SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE)
    private List<SchedulingProducingArrangement> planningArrangementsSequence = new ArrayList<>();

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(new FactoryReadableIdentifier(
                getCategoryName(),
                getSeqNum()
        ));
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public boolean weatherFactoryProducingTypeIsSlot() {
        return getSchedulingFactoryInfo().weatherFactoryProducingTypeIsSlot();
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo()
                .weatherFactoryProducingTypeIsQueue();
    }

    @Override
    public String toString() {
        return "SchedulingFactoryInstance{" + "readableIdentifier='" + factoryReadableIdentifier + '\'' + ", producingLength=" + producingLength +
               ", " +
               "reapWindowSize=" + reapWindowSize + '}';
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo()
                .typeEqual(that.getSchedulingFactoryInfo());
    }

}
