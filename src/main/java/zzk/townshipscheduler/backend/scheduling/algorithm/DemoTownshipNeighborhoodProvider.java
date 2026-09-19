package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.MassChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.builtin.PillarChangeMoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Neighborhoods API（Timefold 2.7.0-rc-1 preview）例程集，全部使用 TownshipScheduler 真实领域对象。
 * API 签名逐项核对自 timefold-solver-core-2.7.0-rc-1-sources.jar，本文件已通过项目全量编译验证。
 * <p>
 * 修订版（v2）：删除旧版 D1-D10 玩具示例，17 个例程按难度递进重排：
 * <pre>{@code
 * 容易档 E1-E5   单变量 change + 单个谓词，只读实体字段/值域 —— 先把 API 骨架摸熟
 * 中等档 M1-M8   swap / 链 / 双变量 compose / 领域约束直击 —— 例程最多的部分，篇幅最大
 * 困难档 H1-H4   内置 provider（Pillar/Mass）/ 自定义 Sampler / iterator 手写流
 * }</pre>
 * <p>
 * 启用方式（solverConfig.xml，包名已修正）：
 * <pre>{@code
 * <solver ...>
 *     <enablePreviewFeature>NEIGHBORHOODS</enablePreviewFeature>
 *     <constructionHeuristic>...</constructionHeuristic>   <!-- LS 要求已初始化解 -->
 *     <localSearch>
 *         <neighborhoodProviderClass>
 *             zzk.townshipscheduler.backend.scheduling.algorithm.DemoTownshipNeighborhoodProvider
 *         </neighborhoodProviderClass>
 *         <acceptor>...</acceptor>   <!-- tabu/SA/LA 与邻域正交，照常配 -->
 *     </localSearch>
 * </solver>
 * }</pre>
 * <p>
 * 验证方式：本文件没有独立 main / 测试类（项目里也没有 NeighborhoodTester）。
 * 把上面的 neighborhoodProviderClass 挂进 solverConfig.xml，跑一次求解，
 * 观察 local search 阶段的 step 日志即可确认 provider 被实例化并产出 move。
 * 单变量归因（你的 benchmark 习惯）：defineNeighborhood 里只保留目标档的 .add(...)，其余注释。
 * <p>
 * 一条贯穿全部例程的领域知识：
 * {@code SolutionView} / {@code MoveIteratorSession} 拿不到原始 solution 对象，谓词里唯一能枚举
 * "同工厂有哪些工序"的通道是 {@code SchedulingFactoryInstance.planningArrangementsSequence}
 * （inverse shadow，结构性 shadow，随变量同步更新，谓词里可安全读）。
 * 而 completedDateTime / computedDateTimePair 是计算型 shadow（依赖整条队列链重算），
 * move 生成阶段读到的可能是旧值 —— 所以本文件一律用静态工期近似，不读计算型 shadow。
 */
public class DemoTownshipNeighborhoodProvider
        implements NeighborhoodProvider<TownshipSchedulingProblem> {

    /**
     * 全开做冒烟测试用；正式对拍时按档注释，保持单变量归因。
     */
    @Override
    public Neighborhood defineNeighborhood(NeighborhoodBuilder<TownshipSchedulingProblem> builder) {
        PlanningSolutionMetaModel<TownshipSchedulingProblem> meta = builder.getSolutionMetaModel();
        var arrangement = meta.genuineEntity(SchedulingProducingArrangement.class);
        var slotVar = arrangement.basicVariable(
                SchedulingProducingArrangement.PLANNING_DATE_TIME_SLOT,
                SchedulingDateTimeSlot.class
        );
        var factoryVar = arrangement.basicVariable(
                SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE,
                SchedulingFactoryInstance.class
        );

        // ============ 容易档 ============
        return builder
                .add(new E1FrameworkValueRangeFactoryChange(factoryVar))
                .add(new E2DomainTypeFactoryChange(factoryVar))
                .add(new E3IdealWindowSlotChange(slotVar))
                .add(new E4SlotDurationFitsChange(slotVar))
                .add(new E5LoadBalancedFactoryChange(factoryVar))
                // ============ 中等档 ============
                .add(new M1SameFactoryNearbySlotSwap(slotVar, factoryVar))
                .add(new M2SameFactoryConflictSlotSwap(slotVar, factoryVar))
                .add(new M3PrerequisiteChainEscape(slotVar))
                .add(new M4CalendarEndEscape(slotVar))
                .add(new M5HighValueDeadlineRescue(slotVar))
                .add(new M6SleepWindowEscape(slotVar))
                .add(new M7ComposedFactorySlotChange(factoryVar, slotVar))
                .add(new M8QueueFactoryMicroAdjust(slotVar, factoryVar))
                // ============ 困难档 ============
                .add(new H1PillarSameSlotGroupChange(slotVar))
                .add(new H2MassChangeRegionAware(factoryVar))
                .add(new H3ChainAncestorSwap(slotVar))
                .add(new H4BestResponseFactorySlotCombo(factoryVar, slotVar))
                .build();
    }

    /* ===================================================================== *
     *                         容 易 档（E1-E5）                              *
     *                                                                        *
     * 训练目标：一个 move 的完整骨架 ——                                *
     *   pick(实体流).pick(值流, 谓词).asMove(构造器)                         *
     * 全部是单变量 change：一道工序换一个值，谓词只读实体字段/值域。          *
     * ===================================================================== */

    /* ====================== E1 ====================== *
     * 换工厂：用框架的值域通道（isValueInRange）。        *
     * 你的 planningFactoryInstance 值域定义在实体上（按产品所需工厂类型过滤）， *
     * isValueInRange 会替你执行 @ValueRangeProvider 的全部逻辑，                *
     * 不生产注定吃 forbidBrokenFactoryAbility 惩罚的废 move。                  *
     * 教学点：最小可用 provider 长什么样 —— 这是后面所有例程的地基。           *
     * ================================================= */
    record E1FrameworkValueRangeFactoryChange(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance> factoryVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return f.pick(f.forEach(SchedulingProducingArrangement.class, false))
                    .pick(
                            f.forEach(SchedulingFactoryInstance.class, false),
                            NeighborhoodsJoiners.filtering(this::legalDifferentFactory)
                    )
                    .asMove((view, arrangement, target) -> Moves.change(factoryVar, arrangement, target));
        }

        private boolean legalDifferentFactory(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingFactoryInstance target
        ) {
            if (target == null) {
                return false;
            }
            SchedulingFactoryInstance current = view.getValue(factoryVar, arrangement);
            return current != target && view.isValueInRange(factoryVar, arrangement, target);
        }

    }

    /* ====================== E2 ====================== *
     * 换工厂：走领域自己的类型通道（requiredFactoryInfo.typeEqual）。 *
     * 与 E1 效果等价，但谓词更便宜（不触发值域计算），且能表达          *
     * isValueInRange 表达不了的"类型相等"语义。                         *
     * 教学点：同一个意图，两种写法。E1 是框架通道，E2 是领域通道，       *
     * 后面 M 档的例程将混用两者 —— 记住它们的分工。                     *
     * ================================================= */
    record E2DomainTypeFactoryChange(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance> factoryVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return f.pick(f.forEach(SchedulingProducingArrangement.class, false))
                    .pick(
                            f.forEach(SchedulingFactoryInstance.class, false),
                            NeighborhoodsJoiners.filtering(this::sameTypeDifferentFactory)
                    )
                    .asMove((view, arrangement, target) -> Moves.change(factoryVar, arrangement, target));
        }

        private boolean sameTypeDifferentFactory(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingFactoryInstance target
        ) {
            if (target == null) {
                return false;
            }
            SchedulingFactoryInstance current = view.getValue(factoryVar, arrangement);
            return current != null && current != target
                   && arrangement.getRequiredFactoryInfo().typeEqual(target.getSchedulingFactoryInfo());
        }

    }

    /* ====================== E3 ====================== *
     * 换槽：只准换到"不早于静态理想开始"的槽。           *
     * 直击 preferArrangeDateTimeAsSoonAsPassible 的下界 ——             *
     * 比理想开始更早的槽一定会吃"开工过早"惩罚，先剪掉。                 *
     * 教学点：calcStaticIdealArrangeDateTime 是约束在领域里的投影，      *
     * 把约束的"事后扣分"翻译成邻域的"事前剪枝"是写 provider 的基本功。   *
     * ================================================= */
    record E3IdealWindowSlotChange(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return f.pick(f.forEach(SchedulingProducingArrangement.class, false))
                    .pick(
                            f.forEach(SchedulingDateTimeSlot.class, false),
                            NeighborhoodsJoiners.filtering(this::notEarlierThanIdeal)
                    )
                    .asMove((view, arrangement, target) -> Moves.change(slotVar, arrangement, target));
        }

        private boolean notEarlierThanIdeal(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingDateTimeSlot target
        ) {
            SchedulingDateTimeSlot current = view.getValue(slotVar, arrangement);
            if (current == null || target == null || current == target) {
                return false;
            }
            LocalDateTime ideal = arrangement.calcStaticIdealArrangeDateTime();
            return ideal != null && !target.getStart().isBefore(ideal);
        }

    }

    /* ====================== E4 ====================== *
     * 换槽：只换到"装得下生产时长"的槽。                 *
     * 你的 SchedulingDateTimeSlot 有 durationInMinute，工序有 producingDuration： *
     * 一个比生产时长还短的槽，挪过去只会立刻违约。                               *
     * 教学点：E3 用 SolutionView 读当前值，本例只读实体字段 ——                 *
     * 实体字段更便宜，且不依赖 view（可在 filter 之前用于更宽的剪枝）。           *
     * ================================================= */
    record E4SlotDurationFitsChange(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return f.pick(f.forEach(SchedulingProducingArrangement.class, false))
                    .pick(
                            f.forEach(SchedulingDateTimeSlot.class, false),
                            NeighborhoodsJoiners.filtering(this::durationFits)
                    )
                    .asMove((view, arrangement, target) -> Moves.change(slotVar, arrangement, target));
        }

        private boolean durationFits(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingDateTimeSlot target
        ) {
            SchedulingDateTimeSlot current = view.getValue(slotVar, arrangement);
            if (current == null || target == null || current == target) {
                return false;
            }
            Duration producing = arrangement.getProducingDuration();
            return producing != null && target.getDurationInMinute() >= producing.toMinutes();
        }

    }

    /* ====================== E5 ====================== *
     * 换工厂：负载均衡 —— 直击 preferLoadBalanceArrangementsInFactoryInstance。 *
     * 把工序换到"同类型里当前负载更低"的工厂。负载 = inverse shadow 的 size()。  *
     * 教学点：inverse shadow 的第一个实际用途。                                  *
     * 注意这是 E 档唯一碰 inverse shadow 的例程 —— 也是后面 M/H 档大量例程的基石。 *
     * ================================================= */
    record E5LoadBalancedFactoryChange(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance> factoryVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return f.pick(f.forEach(SchedulingProducingArrangement.class, false))
                    .pick(
                            f.forEach(SchedulingFactoryInstance.class, false),
                            NeighborhoodsJoiners.filtering(this::isLessLoaded)
                    )
                    .asMove((view, arrangement, target) -> Moves.change(factoryVar, arrangement, target));
        }

        /**
         * 目标工厂必须是同类型、且当前负载严格低于当前工厂。
         */
        private boolean isLessLoaded(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingFactoryInstance target
        ) {
            if (target == null) {
                return false;
            }
            SchedulingFactoryInstance current = view.getValue(factoryVar, arrangement);
            if (current == null || current == target) {
                return false;
            }
            if (!arrangement.getRequiredFactoryInfo().typeEqual(target.getSchedulingFactoryInfo())) {
                return false;
            }
            return loadOf(target) < loadOf(current);
        }

        private static int loadOf(SchedulingFactoryInstance factory) {
            return factory.getPlanningArrangementsSequence() == null
                    ? 0
                    : factory.getPlanningArrangementsSequence().size();
        }

    }

    /* ===================================================================== *
     *                         中 等 档（M1-M8）                              *
     *                                                                        *
     * 训练目标：pair 谓词（两个实体的关系）、链遍历、双变量 compose、         *
     * 以及"把每个约束翻译成邻域"的完整套路。这是例程最多的部分。             *
     * 每例都直击你项目里的一条真实约束。                                     *
     * ===================================================================== */

    /* ====================== M1 ====================== *
     * 同工厂、时间近邻的两道工序对换槽位。               *
     * 背景：job-shop 关键路径邻域（Nowicki-Smulkanda block move）的          *
     * 最小可行替身 —— 真关键路径需要 makespan 归因，这里先要"同厂 + 邻接"   *
     * 的语义正确性。                                                          *
     * 教学点：M 档第一个 swap。同一实体流 pick 两次 = 实体对；              *
     * sa.id < sb.id 保证 a != b 且每对只生成一次（去重惯例，后面反复用）。   *
     * ================================================= */
    record M1SameFactoryNearbySlotSwap(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar,
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance> factoryVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        static final int ADJACENCY_SLOTS = 8;

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            UniEnumeratingStream<TownshipSchedulingProblem, SchedulingProducingArrangement> arrangements =
                    f.forEach(SchedulingProducingArrangement.class, false);
            return f.pick(arrangements)
                    .pick(arrangements, NeighborhoodsJoiners.filtering(this::sameFactoryNearbyOrdered))
                    .asMove((view, a, b) -> Moves.swap(slotVar, a, b));
        }

        private boolean sameFactoryNearbyOrdered(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement a,
                SchedulingProducingArrangement b
        ) {
            SchedulingFactoryInstance fa = view.getValue(factoryVar, a);
            SchedulingFactoryInstance fb = view.getValue(factoryVar, b);
            SchedulingDateTimeSlot sa = view.getValue(slotVar, a);
            SchedulingDateTimeSlot sb = view.getValue(slotVar, b);
            return fa != null && fa == fb && sa != null && sb != null
                   && sa.getId() < sb.getId()
                   && sb.getId() - sa.getId() <= ADJACENCY_SLOTS;
        }

    }

    /* ====================== M2 ====================== *
     * 同工厂、时间窗重叠的两道工序对换槽位 —— 把冲突打散。 *
     * 直击 forbidBrokenFactoryAbility 的重叠维度：同工厂同时进行的工序数     *
     * 超过 producingLength 会吃硬惩罚，而"重叠对"换槽是打散重叠的经典手法。   *
     * 教学点：与 M1 的对比 —— M1 看 id 相邻（结构近似），本例看真实时间窗     *
     * 重叠（语义精确）。静态工期近似：arrange 到 arrange+producingDuration。  *
     * 不读 completedDateTime：那是计算型 shadow，move 生成阶段读到的可能旧值。*
     * ================================================= */
    record M2SameFactoryConflictSlotSwap(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar,
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance> factoryVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            UniEnumeratingStream<TownshipSchedulingProblem, SchedulingProducingArrangement> arrangements =
                    f.forEach(SchedulingProducingArrangement.class, false);
            return f.pick(arrangements)
                    .pick(arrangements, NeighborhoodsJoiners.filtering(this::sameFactoryAndOverlapping))
                    .asMove((view, a, b) -> Moves.swap(slotVar, a, b));
        }

        private boolean sameFactoryAndOverlapping(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement a,
                SchedulingProducingArrangement b
        ) {
            if (a == b) {
                return false;
            }
            SchedulingFactoryInstance fa = view.getValue(factoryVar, a);
            if (fa == null || fa != view.getValue(factoryVar, b)) {
                return false;
            }
            SchedulingDateTimeSlot sa = view.getValue(slotVar, a);
            SchedulingDateTimeSlot sb = view.getValue(slotVar, b);
            if (sa == null || sb == null || sa == sb) {
                return false;
            }
            Duration da = a.getProducingDuration() == null
                    ? Duration.ZERO
                    : a.getProducingDuration();
            Duration db = b.getProducingDuration() == null
                    ? Duration.ZERO
                    : b.getProducingDuration();
            return sa.getStart().isBefore(sb.getStart().plus(db))
                   && sa.getStart().plus(da).isAfter(sb.getStart());
        }

    }

    /* ====================== M3 ====================== *
     * 前置链逃逸：只把工序换到"所有 deepPrerequisite 都完工之后"的槽位。 *
     * 直击 forbidBrokenPrerequisiteArrangement（前置链完成后才能开工）。    *
     * 教学点：谓词里遍历 deepPrerequisiteProducingArrangements 链，          *
     * 用 calcStaticIdealCompleteDateTime 求每个前置的静态完工下界，          *
     * 取最大值作为目标槽的下限。这是 M 档第一个"读关联结构"的例程。        *
     * 与 H3（链内 swap）互补：本例整条链一起后移，H3 在链内部互换。        *
     * ================================================= */
    record M3PrerequisiteChainEscape(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return f.pick(f.forEach(SchedulingProducingArrangement.class, false))
                    .pick(
                            f.forEach(SchedulingDateTimeSlot.class, false),
                            NeighborhoodsJoiners.filtering(this::afterAllPrerequisites)
                    )
                    .asMove((view, arrangement, target) -> Moves.change(slotVar, arrangement, target));
        }

        /**
         * 目标槽的 start 必须不早于所有前置（递归闭包）的静态完工时间。
         */
        private boolean afterAllPrerequisites(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingDateTimeSlot target
        ) {
            SchedulingDateTimeSlot current = view.getValue(slotVar, arrangement);
            if (current == null || target == null || current == target) {
                return false;
            }
            var prerequisites = arrangement.getDeepPrerequisiteProducingArrangements();
            if (prerequisites == null || prerequisites.isEmpty()) {
                return true;
            }
            LocalDateTime latestPrerequisiteCompletion = null;
            for (var prerequisite : prerequisites) {
                if (prerequisite == null) {
                    continue;
                }
                LocalDateTime staticCompletion = prerequisite.calcStaticIdealCompleteDateTime();
                if (staticCompletion == null) {
                    continue;
                }
                if (latestPrerequisiteCompletion == null
                    || staticCompletion.isAfter(latestPrerequisiteCompletion)) {
                    latestPrerequisiteCompletion = staticCompletion;
                }
            }
            return latestPrerequisiteCompletion == null
                   || !target.getStart().isBefore(latestPrerequisiteCompletion);
        }

    }

    /* ====================== M4 ====================== *
     * 日历尾逃逸：完工时间越过工作日历末尾的工序，挪去更早的槽。 *
     * 直击 shouldNotBrokenCalendarEnd。                                  *
     * 教学点："先判断当前是否违约，再要求目标不违约"的聚焦写法 ——         *
     * 只对违约者动手，把邻域火力集中在真正需要修复的地方。                *
     * ================================================= */
    record M4CalendarEndEscape(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return f.pick(f.forEach(SchedulingProducingArrangement.class, false))
                    .pick(
                            f.forEach(SchedulingDateTimeSlot.class, false),
                            NeighborhoodsJoiners.filtering(this::finishesInsideCalendar)
                    )
                    .asMove((view, arrangement, target) -> Moves.change(slotVar, arrangement, target));
        }

        /**
         * 只对"当前会越过日历尾"的工序动手，且目标槽必须在日历内完工。
         */
        private boolean finishesInsideCalendar(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingDateTimeSlot target
        ) {
            SchedulingDateTimeSlot current = view.getValue(slotVar, arrangement);
            if (current == null || target == null || current == target) {
                return false;
            }
            Duration producing = arrangement.getProducingDuration();
            LocalDateTime calendarEnd = arrangement.getWorkCalendarEnd();
            if (producing == null || calendarEnd == null) {
                return false;
            }
            boolean currentlyBreached = current.getStart().plus(producing).isAfter(calendarEnd);
            if (!currentlyBreached) {
                return false;
            }
            return !target.getStart().plus(producing).isAfter(calendarEnd);
        }

    }

    /* ====================== M5 ====================== *
     * 高价值 deadline 救援：有 deadline 且静态估计会违约的工序， *
     * 挪进 [理想开始, deadline - 工期] 可行窗。                      *
     * 直击 shouldNotBrokenDeadlineOrder。                            *
     * 教学点：getEvaluateFactor() 越高（火车/飞机单）越该被救 ——       *
     * 把"订单重要性"从打分层搬进邻域层，让 move 生成器有主见。         *
     * 与 E3 的对比：E3 只剪下界，本例剪上下界且只对违约者动手。        *
     * ================================================= */
    record M5HighValueDeadlineRescue(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        static final int MIN_EVALUATE_FACTOR = 100; // 只救火车/飞机这种高权重单

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return f.pick(f.forEach(SchedulingProducingArrangement.class, false))
                    .pick(
                            f.forEach(SchedulingDateTimeSlot.class, false),
                            NeighborhoodsJoiners.filtering(this::rescueDeadline)
                    )
                    .asMove((view, arrangement, target) -> Moves.change(slotVar, arrangement, target));
        }

        private boolean rescueDeadline(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingDateTimeSlot target
        ) {
            SchedulingDateTimeSlot current = view.getValue(slotVar, arrangement);
            if (current == null || target == null || current == target) {
                return false;
            }
            if (arrangement.getEvaluateFactor() < MIN_EVALUATE_FACTOR) {
                return false;
            }
            LocalDateTime deadline;
            try {
                deadline = arrangement.boolHasDeadline()
                        ? arrangement.getDeadline()
                        : null;
            } catch (NullPointerException e) {
                return false; // 非订单直属工序无 deadline
            }
            if (deadline == null) {
                return false;
            }
            Duration producing = arrangement.getProducingDuration();
            LocalDateTime ideal = arrangement.calcStaticIdealArrangeDateTime();
            if (producing == null || ideal == null) {
                return false;
            }
            boolean currentlyBreached = current.getStart().plus(producing).isAfter(deadline);
            if (!currentlyBreached) {
                return false;
            }
            return !target.getStart().isBefore(ideal)
                   && !target.getStart().plus(producing).isAfter(deadline);
        }

    }

    /* ====================== M6 ====================== *
     * 睡眠窗逃逸：当前槽落在玩家睡眠期的工序，一步跳进醒来后的 recovery 窗。 *
     * 直击 preferNotArrangeInPlayerSleepTime（你模型里最有领域特色的一条软约束）。 *
     * 教学点：UniEnumeratingStream#filter —— 在 pick 之前先过滤实体流，      *
     * 与"pick 之后用谓词剪值"互补：filter 削减的是被 pick 的实体数。         *
     * 跨午夜处理：sleepEnd 早于 sleepStart 表示睡眠跨越午夜，两个分支写法不同。 *
     * ================================================= */
    record M6SleepWindowEscape(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        static final int RECOVERY_WINDOW_MINUTES = 240;

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            // filter 作用于"实体流"：只留下当前槽在睡眠期内的工序
            var inSleep = f.forEach(SchedulingProducingArrangement.class, false)
                    .filter(this::currentlyInSleep);

            return f.pick(inSleep)
                    .pick(
                            f.forEach(SchedulingDateTimeSlot.class, false),
                            NeighborhoodsJoiners.filtering(this::inRecoveryWindow)
                    )
                    .asMove((view, arrangement, target) -> Moves.change(slotVar, arrangement, target));
        }

        private boolean currentlyInSleep(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement
        ) {
            SchedulingDateTimeSlot current = view.getValue(slotVar, arrangement);
            if (current == null) {
                return false;
            }
            LocalTime at = current.getStart().toLocalTime();
            LocalTime sleepStart = arrangement.getSchedulingPlayer().getSleepStart();
            LocalTime sleepEnd = arrangement.getSchedulingPlayer().getSleepEnd();
            return sleepEnd.isAfter(sleepStart)
                    ? at.isAfter(sleepStart) && at.isBefore(sleepEnd)
                    : at.isAfter(sleepStart) || at.isBefore(sleepEnd); // 跨午夜
        }

        /**
         * 目标必须是"醒来后 RECOVERY_WINDOW 内"的槽（同样处理跨午夜）。
         */
        private boolean inRecoveryWindow(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingDateTimeSlot target
        ) {
            if (target == null) {
                return false;
            }
            LocalTime tt = target.getStart().toLocalTime();
            LocalTime sleepEnd = arrangement.getSchedulingPlayer().getSleepEnd();
            boolean afterWake = sleepEnd.isAfter(arrangement.getSchedulingPlayer().getSleepStart())
                    ? !tt.isBefore(sleepEnd)
                    : tt.isAfter(arrangement.getSchedulingPlayer().getSleepStart()) || !tt.isBefore(sleepEnd);
            return afterWake && minutesBetween(sleepEnd, tt) <= RECOVERY_WINDOW_MINUTES;
        }

        private static int minutesBetween(LocalTime from, LocalTime to) {
            int d = to.toSecondOfDay() - from.toSecondOfDay();
            return d >= 0
                    ? d / 60
                    : (24 * 3600 + d) / 60;
        }

    }

    /* ====================== M7 ====================== *
     * 组合双变量原子 change —— Moves.compose 的用法。  *
     * 一次 move 同时换 factory + slot，两个变量一起变才算一次变更：        *
     * 一个 tabu 条目、一次撤销、一次 score 重算。                           *
     * 教学点：compose vs swap 的区别 —— swap 是两人互换同一变量；          *
     * compose 是"给同一个人换两样东西"。                                  *
     * 场景：换到同类型里负载更低的工厂，同时保住当前槽位（保槽换厂）。     *
     * 实际应用时把第二个 change 换成你的真实意图（例如 E3 的理想窗槽）。   *
     * ================================================= */
    record M7ComposedFactorySlotChange(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance> factoryVar,
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return f.pick(f.forEach(SchedulingProducingArrangement.class, false))
                    .pick(
                            f.forEach(SchedulingFactoryInstance.class, false),
                            NeighborhoodsJoiners.filtering(this::isLessLoaded)
                    )
                    .asMove((view, arrangement, targetFactory) -> {
                        SchedulingDateTimeSlot currentSlot = view.getValue(slotVar, arrangement);
                        return Moves.compose(
                                Moves.change(factoryVar, arrangement, targetFactory),
                                Moves.change(slotVar, arrangement, currentSlot) // 保槽；换成你的真实目标槽即可
                        );
                    });
        }

        private boolean isLessLoaded(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingFactoryInstance target
        ) {
            if (target == null) {
                return false;
            }
            SchedulingFactoryInstance current = view.getValue(factoryVar, arrangement);
            if (current == null || current == target) {
                return false;
            }
            if (!arrangement.getRequiredFactoryInfo().typeEqual(target.getSchedulingFactoryInfo())) {
                return false;
            }
            return loadOf(target) < loadOf(current);
        }

        private static int loadOf(SchedulingFactoryInstance factory) {
            return factory.getPlanningArrangementsSequence() == null
                    ? 0
                    : factory.getPlanningArrangementsSequence().size();
        }

    }

    /* ====================== M8 ====================== *
     * 队列型工厂专用换槽 —— ProducingStructureType 的用法。 *
     * 你的工厂分两类：SLOT 型（并行槽，可同时多工序）和 QUEUE 型           *
     * （串行队列，同一时刻只能一道工序按序走）。两类工厂的邻域语义不同：    *
     * 队列里"时间越早 = 越靠前"，乱序换槽会破坏队列顺序，所以本 provider   *
     * 只允许 QUEUE 型工厂上的工序往前挪（优化顺序），不许跳回后面。        *
     * 教学点：用领域类别把邻域细分 —— 一个 provider 不必服务所有实体。     *
     * ================================================= */
    record M8QueueFactoryMicroAdjust(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar,
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance> factoryVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return f.pick(f.forEach(SchedulingProducingArrangement.class, false))
                    .pick(
                            f.forEach(SchedulingDateTimeSlot.class, false),
                            NeighborhoodsJoiners.filtering(this::queueFactoryEarlierSlot)
                    )
                    .asMove((view, arrangement, target) -> Moves.change(slotVar, arrangement, target));
        }

        /**
         * 只处理"当前在队列型工厂上"的工序，且目标槽必须更早（队列内部微调）。
         */
        private boolean queueFactoryEarlierSlot(
                SolutionView<TownshipSchedulingProblem> view,
                SchedulingProducingArrangement arrangement,
                SchedulingDateTimeSlot target
        ) {
            SchedulingFactoryInstance factory = view.getValue(factoryVar, arrangement);
            if (factory == null || !factory.weatherFactoryProducingTypeIsQueue()) {
                return false;
            }
            SchedulingDateTimeSlot current = view.getValue(slotVar, arrangement);
            return current != null && target != null && current != target
                   && target.getId() < current.getId();
        }

    }

    /* ===================================================================== *
     *                         困 难 档（H1-H4）                              *
     *                                                                        *
     * 训练目标：内置 provider 的正确构造（Pillar/Mass）、自定义 Sampler、    *
     * 以及完全手写的 iterator 流。                                            *
     * 前两个例程直接修你 TownshipSchedulingNeighborhoodProvider 里的两个配置错误。 *
     * ===================================================================== */

    /* ====================== H1 ====================== *
     * PillarChangeMoveProvider：同一槽位上的整组工序一起换槽。 *
     * 背景：你的 TownshipSchedulingNeighborhoodProvider 里写了                *
     *   MassChangeMoveProvider<>(dataTimeVariableMeta, Samplers.all())        *
     * 两个问题：(1) Samplers.all() 让单次 move 成本随数据集线性增长；          *
     *           (2) MassChange 在 entity 级、互不相交的值域上系统性失败（见 H2）。*
     * Pillar 的替代思路：按"共享规划值"分组 —— 同一 planningDateTimeSlot 上的 *
     * 所有工序天然共享一个值域（该槽的后续空位），组内采样不会跨域。          *
     * 教学点：内置 provider 的正确构造参数，而不是自己造轮子。                *
     * 规模控制变体：PillarChangeMoveProvider 整组全挪；要限制组大小用        *
     *   new SubPillarChangeMoveProvider<>(var, Samplers.pillar(Samplers.between(2, 4))) *
     * （subpillar 对值域是"组内子集"，配 Samplers.all() 等于自废限制，别学）。 *
     * ================================================= */
    record H1PillarSameSlotGroupChange(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            // Pillar = 共享同一规划值的实体集合；这里即"同一槽位的所有工序"
            return new PillarChangeMoveProvider<>(slotVar).build(f);
        }

    }

    /* ====================== H2 ====================== *
     * MassChange + region-aware Sampler —— 修你模型上的 mass 系统性失败。 *
     * 背景：你的 factory 值域是 entity 级、按 requiredFactoryInfo 类型过滤、  *
     * 互不相交。MassChange 抽到跨类型组 → 值域交集为空 → 抽取预算耗光 → 失败。 *
     * 修复：自写 Sampler，一个 sample 只收同 requiredFactoryInfo 的成员 ——    *
     * 第一个被 ACCEPT 的成员定下"锚点类型"，之后 REJECT 所有异类型候选。      *
     * 教学点：Sampler 的 reset / minimumSize / targetSize / evaluate 契约。    *
     * 注意 import：SchedulingFactoryInfo 在 model 包（早期版本曾漏 import 导致编译失败）。 *
     * ================================================= */
    record H2MassChangeRegionAware(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance> factoryVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            return new MassChangeMoveProvider<>(factoryVar, new SameRequiredFactoryInfoSampler(2, 4)).build(f);
        }

        /**
         * Region-aware sampler：一个 sample 只收同 requiredFactoryInfo 的成员。
         */
        static final class SameRequiredFactoryInfoSampler
                implements Sampler<SchedulingProducingArrangement> {

            private final int min;

            private final int max;

            private SchedulingFactoryInfo anchorInfo;

            private int target;

            SameRequiredFactoryInfoSampler(int min, int max) {
                this.min = min;
                this.max = max;
            }

            @Override
            public void reset(RandomGenerator random) {
                anchorInfo = null;
                target = min + random.nextInt(max - min + 1);
            }

            @Override
            public int minimumSize() {
                return min;
            }

            @Override
            public int targetSize() {
                return target;
            }

            @Override
            public Sample.Decision evaluate(int sizeSoFar, SchedulingProducingArrangement candidate) {
                if (candidate == null) {
                    return Sample.Decision.STOP;
                }
                if (anchorInfo == null) {
                    anchorInfo = candidate.getRequiredFactoryInfo();
                    if (anchorInfo == null) {
                        return Sample.Decision.STOP;
                    }
                }
                if (!anchorInfo.typeEqual(candidate.getRequiredFactoryInfo())) {
                    return Sample.Decision.REJECT;
                }
                return sizeSoFar + 1 >= target
                        ? Sample.Decision.ACCEPT_AND_STOP
                        : Sample.Decision.ACCEPT;
            }

        }

    }

    /* ====================== H3 ====================== *
     * iterator + asCachedDataset：每道工序与它 deepPrerequisite 链里的     *
     * 随机一个祖先互换槽位。                                                  *
     * 背景：标准 pillar move 按"规划值相同"分组，永远够不着链维度           *
     * （链成员从不共享槽位）—— 这正是你 benchmark 里 pillar 加了没用、       *
     * 删了又加回来的根因。链内互换必须手写 iterator。                        *
     * 教学点：asCachedDataset() + buildMoveStream(MoveIteratorProvider)     *
     *          + session.getInstance(dataset).exhaustiveIterator(random)。  *
     * 仍用内置 Moves.swap 构造 move => rebase / tabu 能力免费继承。          *
     * ================================================= */
    record H3ChainAncestorSwap(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        static final int MAX_MOVES_PER_SESSION = 256;

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            var dataset = f.forEach(SchedulingProducingArrangement.class, false).asCachedDataset();

            return f.buildMoveStream((session, random) -> {
                var view = session.getSolutionView();
                var rows = session.getInstance(dataset).exhaustiveIterator(random);
                List<Move<TownshipSchedulingProblem>> moves = new ArrayList<>();
                while (rows.hasNext() && moves.size() < MAX_MOVES_PER_SESSION) {
                    SchedulingProducingArrangement head = rows.next();
                    if (head == null || view.getValue(slotVar, head) == null) {
                        continue;
                    }
                    var ancestors = head.getDeepPrerequisiteProducingArrangements();
                    if (ancestors == null || ancestors.isEmpty()) {
                        continue;
                    }
                    var picked = new ArrayList<>(ancestors);
                    SchedulingProducingArrangement ancestor = picked.get(random.nextInt(picked.size()));
                    if (view.getValue(slotVar, ancestor) == null) {
                        continue;
                    }
                    moves.add(Moves.swap(slotVar, head, ancestor));
                }
                return moves.iterator();
            });
        }

    }

    /* ====================== H4 ====================== *
     * iterator + 双数据集 best-response：对每道工序找                   *
     * "同类型负载最低的工厂 × 静态完工最早的槽"的组合。                    *
     * 教学点：当组合逻辑复杂到 join 表达不了（要按候选做 argmax），        *
     * 就退到 buildMoveStream 手写 —— 两个 dataset instance 各自          *
     * exhaustiveIterator，组合是"每实体两次全扫描"。                      *
     * 每步都是确定性最优（给定随机遍历序），是"生成器有主见"的极致。       *
     * no-op 兜底：无合法工厂/槽时吐"换到当前值"的 no-op move，             *
     * 保持 iterator 良构（不吐 null，不让框架以为流结束了）。              *
     * ================================================= */
    record H4BestResponseFactorySlotCombo(
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance> factoryVar,
            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> slotVar
    )
            implements MoveProvider<TownshipSchedulingProblem> {

        @Override
        public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> f) {
            var factories = f.forEach(SchedulingFactoryInstance.class, false).asCachedDataset();
            var slots = f.forEach(SchedulingDateTimeSlot.class, false).asCachedDataset();

            return f.buildMoveStream((session, random) -> {
                var view = session.getSolutionView();
                var factoryInstance = session.getInstance(factories);
                var slotInstance = session.getInstance(slots);
                var arrangementDataset = f.forEach(SchedulingProducingArrangement.class, false).asCachedDataset();
                var arrangementInstance = session.getInstance(arrangementDataset);

                return new Iterator<Move<TownshipSchedulingProblem>>() {
                    private final Iterator<SchedulingProducingArrangement> rows =
                            arrangementInstance.exhaustiveIterator(random);

                    @Override
                    public boolean hasNext() {
                        return rows.hasNext();
                    }

                    @Override
                    public Move<TownshipSchedulingProblem> next() {
                        SchedulingProducingArrangement arrangement = rows.next();

                        // 1) 同类型里负载最低的工厂
                        SchedulingFactoryInstance bestFactory = null;
                        int bestLoad = Integer.MAX_VALUE;
                        var factoryIterator = factoryInstance.exhaustiveIterator(random);
                        while (factoryIterator.hasNext()) {
                            SchedulingFactoryInstance candidate = factoryIterator.next();
                            if (candidate == null
                                || !arrangement.getRequiredFactoryInfo().typeEqual(candidate.getSchedulingFactoryInfo())) {
                                continue;
                            }
                            int load = candidate.getPlanningArrangementsSequence() == null
                                    ? 0
                                    : candidate.getPlanningArrangementsSequence().size();
                            if (load < bestLoad) {
                                bestLoad = load;
                                bestFactory = candidate;
                            }
                        }

                        // 2) 静态完工最早的槽
                        SchedulingDateTimeSlot bestSlot = null;
                        LocalDateTime bestCompletion = null;
                        Duration producing = arrangement.getProducingDuration();
                        if (producing != null) {
                            var slotIterator = slotInstance.exhaustiveIterator(random);
                            while (slotIterator.hasNext()) {
                                SchedulingDateTimeSlot candidate = slotIterator.next();
                                if (candidate == null) {
                                    continue;
                                }
                                LocalDateTime completion = candidate.getStart().plus(producing);
                                if (bestCompletion == null || completion.isBefore(bestCompletion)) {
                                    bestCompletion = completion;
                                    bestSlot = candidate;
                                }
                            }
                        }

                        // 无合法组合就吐 no-op，保持流良构
                        if (bestFactory == null || bestSlot == null) {
                            return Moves.change(slotVar, arrangement, view.getValue(slotVar, arrangement));
                        }
                        return Moves.compose(
                                Moves.change(factoryVar, arrangement, bestFactory),
                                Moves.change(slotVar, arrangement, bestSlot)
                        );
                    }
                };
            });
        }

    }

}
