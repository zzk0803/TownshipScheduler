//package zzk.townshipscheduler.backend.scheduling;
//
//import ai.timefold.solver.core.api.solver.SolverFactory;
//import ai.timefold.solver.core.api.solver.SolverManager;
//import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
//import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
//import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
//import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
//import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
//import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;
//import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
//import ai.timefold.solver.core.config.localsearch.LocalSearchType;
//import ai.timefold.solver.core.config.solver.SolverConfig;
//import ai.timefold.solver.core.config.solver.SolverManagerConfig;
//import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import zzk.townshipscheduler.backend.scheduling.model.AbstractPlayerProducingArrangement;
//
//import java.time.Duration;
//
//@Configuration
//public class TownshipSchedulingConfiguration {
//
//    @Bean
//    public SolverConfig solverConfig() {
//        SolverConfig solverConfig = new SolverConfig();
//        solverConfig.withPhases(
//                new LocalSearchPhaseConfig()
//                        .withMoveSelectorConfig(
//                                new ChangeMoveSelectorConfig()
//                                        .withEntitySelectorConfig(
//                                                new EntitySelectorConfig(AbstractPlayerProducingArrangement.class)
//                                        )
//                                        .withValueSelectorConfig(
//                                                new ValueSelectorConfig("planningDateTimeSlot")
//                                        )
//                        )
//                        .withMoveSelectorConfig(
//                                new ChangeMoveSelectorConfig()
//                                        .withEntitySelectorConfig(
//                                                new EntitySelectorConfig(AbstractPlayerProducingArrangement.class)
//                                        )
//                                        .withValueSelectorConfig(
//                                                new ValueSelectorConfig("planningSequence")
//                                        )
//                        )
//        );
//        return solverConfig;
//    }
//
//}
