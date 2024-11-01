package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import zzk.townshipscheduler.port.GoodId;

import java.util.List;
import java.util.Objects;

@PlanningEntity
public class SchedulingPlantFieldSlot {

    private String category;

    private int categorySeq;

    private List<SchedulingGoods> portfolioGoods;

    private int parallel;

    @InverseRelationShadowVariable(sourceVariableName = "plantSlot")
    private List<SchedulingProducing> schedulingProducingList;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCategorySeq() {
        return categorySeq;
    }

    public void setCategorySeq(int categorySeq) {
        this.categorySeq = categorySeq;
    }

    public List<SchedulingGoods> getPortfolioGoods() {
        return portfolioGoods;
    }

    public void setPortfolioGoods(List<SchedulingGoods> portfolioGoods) {
        this.portfolioGoods = portfolioGoods;
    }

    public int getParallel() {
        return parallel;
    }

    public void setParallel(int parallel) {
        this.parallel = parallel;
    }

    public List<SchedulingProducing> getSchedulingProducingList() {
        return schedulingProducingList;
    }

    public void setSchedulingProducingList(List<SchedulingProducing> schedulingProducingList) {
        this.schedulingProducingList = schedulingProducingList;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchedulingPlantFieldSlot slot)) return false;

        return getCategorySeq() == slot.getCategorySeq() && Objects.equals(getCategory(), slot.getCategory());
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(getCategory());
        result = 31 * result + getCategorySeq();
        return result;
    }

}
