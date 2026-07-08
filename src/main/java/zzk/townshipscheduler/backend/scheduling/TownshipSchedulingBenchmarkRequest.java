package zzk.townshipscheduler.backend.scheduling;

import lombok.*;

@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class TownshipSchedulingBenchmarkRequest {

    @EqualsAndHashCode.Include
    private String problemId;

    @Builder.Default
    private BenchmarkSize benchmarkSize=BenchmarkSize.SELF;

    @Builder.Default
    private BenchmarkStrategy benchmarkStrategy=BenchmarkStrategy.NIGHTLY_RESEARCH;

    public static enum BenchmarkSize {
        SELF(1), SMALL(3), BIG(5);

        private int problemSize;

        BenchmarkSize(int problemSize) {
            this.problemSize = problemSize;
        }

        public int getProblemSize() {
            return problemSize;
        }
    }

    public static enum BenchmarkStrategy {
        NIGHTLY_RESEARCH,
        BUILTIN,
        CONSTRUCTION_HEURISTIC_WITH_AND_WITHOUT_LOCAL_SEARCH,
        EVERY_CONSTRUCTION_HEURISTIC_TYPE,
        EVERY_LOCAL_SEARCH_TYPE,
        EVERY_CONSTRUCTION_HEURISTIC_TYPE_WITH_EVERY_LOCAL_SEARCH_TYPE,
    }

}
