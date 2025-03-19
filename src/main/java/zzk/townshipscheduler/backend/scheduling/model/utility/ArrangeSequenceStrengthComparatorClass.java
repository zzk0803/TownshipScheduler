package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInfo;

import java.util.Comparator;

public class ArrangeSequenceStrengthComparatorClass implements Comparator<SchedulingFactoryInfo.ArrangeSequence> {

    @Override
    public int compare(SchedulingFactoryInfo.ArrangeSequence o1, SchedulingFactoryInfo.ArrangeSequence o2) {
        CompareToBuilder compareToBuilder = new CompareToBuilder();
        compareToBuilder.append(o1, o2, Comparator.comparingInt(SchedulingFactoryInfo.ArrangeSequence::getSequence));
        return compareToBuilder.toComparison();
    }

}
