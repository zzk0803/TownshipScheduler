//package zzk.townshipscheduler.ui.pojo;
//
//import lombok.Builder;
//import lombok.EqualsAndHashCode;
//import lombok.Value;
//import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingBenchmarkRequest;
//
//import java.util.EnumSet;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Value
//@Builder
//@EqualsAndHashCode(onlyExplicitlyIncluded = true)
//public class TownshipSchedulingBenchmarkRequestForm {
//
//    public static final Set<String> VALUE_RANGE_FOR_BENCHMARK_SIZE
//            = EnumSet.allOf(TownshipSchedulingBenchmarkRequest.BenchmarkSize.class)
//            .stream()
//            .map(Enum::name)
//            .collect(Collectors.toSet());
//
//    public static final Set<String> VALUE_RANGE_FOR_BENCHMARK_STRATEGY
//            = EnumSet.allOf(TownshipSchedulingBenchmarkRequest.BenchmarkStrategy.class)
//            .stream()
//            .map(Enum::name)
//            .collect(Collectors.toSet());
//
//    private String problemId;
//
//    private String benchmarkSize = TownshipSchedulingBenchmarkRequest.BenchmarkSize.SELF.name();
//
//    private String benchmarkStrategy = TownshipSchedulingBenchmarkRequest.BenchmarkStrategy.BUILTIN.name();
//
//}
