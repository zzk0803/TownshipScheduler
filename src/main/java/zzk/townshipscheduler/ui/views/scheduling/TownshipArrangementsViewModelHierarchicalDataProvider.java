package zzk.townshipscheduler.ui.views.scheduling;

import com.vaadin.flow.data.provider.hierarchy.AbstractHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import zzk.townshipscheduler.ui.pojo.scheduling.SchedulingProducingArrangementViewModel;

import java.util.List;
import java.util.stream.Stream;

public class TownshipArrangementsViewModelHierarchicalDataProvider extends AbstractHierarchicalDataProvider<SchedulingProducingArrangementViewModel, Void> {

    private List<SchedulingProducingArrangementViewModel> schedulingProducingArrangementViewModels;

    public TownshipArrangementsViewModelHierarchicalDataProvider(List<SchedulingProducingArrangementViewModel> schedulingProducingArrangementViewModels) {
        this.schedulingProducingArrangementViewModels = schedulingProducingArrangementViewModels;
    }

    @Override
    public int getChildCount(HierarchicalQuery<SchedulingProducingArrangementViewModel, Void> query) {
        return query.getParentOptional().map(schedulingProducingArrangementViewModel -> schedulingProducingArrangementViewModel.prerequisiteProducingArrangements().size()).orElse(0);
    }

    @Override
    public Stream<SchedulingProducingArrangementViewModel> fetchChildren(HierarchicalQuery<SchedulingProducingArrangementViewModel, Void> query) {
        return query.getParentOptional()
                .map(SchedulingProducingArrangementViewModel::prerequisiteProducingArrangements)
                .map(ids -> schedulingProducingArrangementViewModels.stream()
                        .filter(schedulingProducingArrangementViewModel -> ids.contains(schedulingProducingArrangementViewModel.arrangementViewModelId())))
                .orElse(Stream.<SchedulingProducingArrangementViewModel>builder().build());
    }

    @Override
    public boolean hasChildren(SchedulingProducingArrangementViewModel item) {
        return !item.prerequisiteProducingArrangements().isEmpty();
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

}
