package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.vaadin.flow.server.VaadinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.TownshipProblemEntity;
import zzk.townshipscheduler.backend.persistence.dao.TownshipProblemEntityRepository;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TownshipSchedulingServiceImpl
        implements ITownshipSchedulingService {

    private final SolverManager<TownshipSchedulingProblem> solverManager;

    private final TownshipProblemEntityRepository townshipProblemEntityRepository;

    private final Map<String, TownshipSchedulingRequest> problemIdRequestMap = new ConcurrentHashMap<>();

    private final Map<String, TownshipSchedulingProblem> idProblemMap = new ConcurrentHashMap<>();

    private final Map<String, SolverJob<TownshipSchedulingProblem>> idSolverJobMap = new ConcurrentHashMap<>();

    private final Consumer<TownshipSchedulingProblem> defaultConsumer = townshipSchedulingProblem -> {
        var uuid = townshipSchedulingProblem.getUuid();
        idProblemMap.put(uuid, townshipSchedulingProblem);
    };

    private final BiConsumer<Object, Throwable> defaultExceptionHandler = (uuid, throwable) -> {
        log.error("problem {} exception {}", uuid, throwable);
    };

    private final TransactionTemplate transactionTemplate;

    @Override
    public TownshipSchedulingProblem prepareScheduling(TownshipSchedulingRequest townshipSchedulingRequest) {
        ProblemTransferProcess process = new ProblemTransferProcess(townshipSchedulingRequest);
        TownshipSchedulingProblem townshipSchedulingProblem = process.buildProblem();
        var uuid = townshipSchedulingProblem.getUuid();
        idProblemMap.put(uuid, townshipSchedulingProblem);
        problemIdRequestMap.put(uuid, townshipSchedulingRequest);
        return townshipSchedulingProblem;
    }

    @Override
    public boolean existSolvingJob(String problemId) {
        return this.idSolverJobMap.containsKey(problemId);
    }

    @Override
    public boolean existProblem(String problemId) {
        return this.idProblemMap.containsKey(problemId) || this.townshipProblemEntityRepository.existsById(problemId);
    }

    public Path searchFile(Path startDir, String targetFileName)
            throws IOException {
        AtomicReference<Path> foundFile = new AtomicReference<>(null);

        Files.walkFileTree(
                startDir, new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.getFileName()
                                .toString()
                                .equalsIgnoreCase(targetFileName)) {
                            foundFile.set(file);
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        if (exc instanceof AccessDeniedException) {
                            return FileVisitResult.CONTINUE;
                        }
                        throw new RuntimeException("visitFileFailed: " + file, exc);
                    }
                }
        );

        return foundFile.get();
    }

    @Override
    public String getProblemSizeStatistics(String problemId) {
        SolverJob<TownshipSchedulingProblem> solverJob = this.idSolverJobMap.get(problemId);
        if (solverJob == null) {
            return "";
        } else {
            return solverJob.getProblemSizeStatistics()
                    .approximateProblemScaleAsFormattedString();
        }
    }

    @Override
    public SolverJob<TownshipSchedulingProblem> scheduling(
            String problemId,
            Consumer<TownshipSchedulingProblem> solverJobStartedEventConsumer,
            Consumer<TownshipSchedulingProblem> firstInitializedSolutionConsumer,
            Consumer<TownshipSchedulingProblem> bestSolutionEventConsumer,
            Consumer<TownshipSchedulingProblem> finalBestSolutionEventConsumer,
            BiConsumer<Object, Throwable> exceptionHandler
    ) {
        SolverJob<TownshipSchedulingProblem> solverJob = solverManager.solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(o -> this.getProblem(problemId))
                .withSolverJobStartedEventConsumer(solverJobStartedEvent -> {
                    TownshipSchedulingProblem solution = solverJobStartedEvent.solution();
                    solverJobStartedEventConsumer.accept(solution);
                })
                .withFirstInitializedSolutionEventConsumer(firstInitializedSolutionEvent -> {
                    TownshipSchedulingProblem townshipSchedulingProblem = firstInitializedSolutionEvent.solution();
                    firstInitializedSolutionConsumer.andThen(defaultConsumer).accept(townshipSchedulingProblem);
                })
                .withBestSolutionEventConsumer(solutionNewBestSolutionEvent -> {
                    TownshipSchedulingProblem solution = solutionNewBestSolutionEvent.solution();
                    defaultConsumer.andThen(bestSolutionEventConsumer)
                            .accept(solution);
                })
                .withFinalBestSolutionEventConsumer(finalBestSolutionEvent -> {
                    defaultConsumer.andThen(finalBestSolutionEventConsumer)
//                                    .andThen(this::persist)
                            .accept(finalBestSolutionEvent.solution());
                })
                .withExceptionHandler(defaultExceptionHandler.andThen(exceptionHandler))
                .run();
        idSolverJobMap.put(problemId, solverJob);
        return solverJob;
    }

    @Override
    public TownshipSchedulingProblem getProblem(String problemId) {
        return this.idProblemMap.get(problemId);
    }

    @Override
    public void abort(String problemId) {
        if (Objects.isNull(problemId) || problemId.isBlank()) {
            return;
        }

        solverManager.terminateEarly(problemId);
    }

    @Override
    public CompletableFuture<File> benchmark(TownshipSchedulingBenchmarkRequest benchmarkRequest) {
        PlannerBenchmarkFactory benchmarkFactory = buildBenchmarkFactory(benchmarkRequest);
        PlannerBenchmark plannerBenchmark = benchmarkFactory.buildPlannerBenchmark(buildBenchmarkProblems(benchmarkRequest));
        return CompletableFuture.supplyAsync(
                        plannerBenchmark::benchmark,
                        VaadinService.getCurrent()
                                .getExecutor()
                )
                .thenComposeAsync(
                        parentDir -> {
                            try {
                                return CompletableFuture.completedFuture(findMostRecentBenchmarkFile(parentDir).orElseThrow());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        VaadinService.getCurrent()
                                .getExecutor()
                );
    }

    private @NonNull PlannerBenchmarkFactory buildBenchmarkFactory(TownshipSchedulingBenchmarkRequest benchmarkRequest) {
        TownshipSchedulingBenchmarkRequest.BenchmarkStrategy benchmarkStrategy = benchmarkRequest.getBenchmarkStrategy();
        return switch (benchmarkStrategy) {
            case BUILTIN -> PlannerBenchmarkFactory.createFromXmlResource(
                    "solverBenchmarkConfig.xml",
                    this.getClass()
                            .getClassLoader()
            );

            case NIGHTLY_RESEARCH -> PlannerBenchmarkFactory.createFromXmlResource(
                    "solverBenchmarkConfig_nightly_research.xml",
                    this.getClass()
                            .getClassLoader()
            );

            case EVERY_CONSTRUCTION_HEURISTIC_TYPE_WITH_EVERY_LOCAL_SEARCH_TYPE -> PlannerBenchmarkFactory.createFromXmlResource(
                    "solverBenchmarkConfig_every_construction_heuristic_type_with_every_local_search_type.xml",
                    this.getClass()
                            .getClassLoader()
            );

            case EVERY_LOCAL_SEARCH_TYPE -> PlannerBenchmarkFactory.createFromXmlResource(
                    "solverBenchmarkConfig_every_local_search_type.xml",
                    this.getClass()
                            .getClassLoader()
            );

            case EVERY_CONSTRUCTION_HEURISTIC_TYPE -> PlannerBenchmarkFactory.createFromXmlResource(
                    "solverBenchmarkConfig_every_construction_heuristic_type.xml",
                    this.getClass()
                            .getClassLoader()
            );

            case CONSTRUCTION_HEURISTIC_WITH_AND_WITHOUT_LOCAL_SEARCH -> PlannerBenchmarkFactory.createFromXmlResource(
                    "solverBenchmarkConfig_construction_heuristic_with_and_without_local_search.xml",
                    this.getClass()
                            .getClassLoader()
            );
        };
    }

    private List<TownshipSchedulingProblem> buildBenchmarkProblems(TownshipSchedulingBenchmarkRequest benchmarkRequest) {
        List<TownshipSchedulingProblem> benchmarkProblems = new ArrayList<>();
        benchmarkProblems.add(getProblem(benchmarkRequest.getProblemId()));
        TownshipSchedulingRequest originalQuest = problemIdRequestMap.get(benchmarkRequest.getProblemId());

        TownshipSchedulingBenchmarkRequest.BenchmarkSize benchmarkSize = benchmarkRequest.getBenchmarkSize();
        if (benchmarkSize == TownshipSchedulingBenchmarkRequest.BenchmarkSize.SELF) {
            return benchmarkProblems;
        }

        int problemSize = benchmarkSize.getProblemSize();
        for (int i = 0; i < problemSize; i++) {
            int factor = i + 1;
            try {
                TownshipSchedulingRequest moreScale = originalQuest.clone();
                moreScale.getPlayerEntityOrderEntities()
                        .forEach(orderEntity -> {
                            Map<ProductEntity, Integer> productAmountMap = orderEntity.getProductAmountMap();
                            productAmountMap.keySet()
                                    .forEach(productEntity -> productAmountMap.computeIfPresent(productEntity, (inMapProduct, integer) -> integer + (factor + factor / 2)));
                        });
                benchmarkProblems.add(prepareBenchmarkScheduling(moreScale));
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        return benchmarkProblems;
    }

    private Optional<File> findMostRecentBenchmarkFile(File parentDir)
            throws IOException {
        File[] directories = parentDir.listFiles(File::isDirectory);
        if (directories == null || directories.length == 0) {
            throw new RuntimeException("No benchmark directories found in " + parentDir.getAbsolutePath());
        }

        Arrays.sort(
                directories,
                Comparator.comparingLong(File::lastModified)
                        .reversed()
        );

        File directory = directories[0];
//        Path path = searchFile(directory.toPath(), BENCHMARK_REPORT_FILE_NAME);
        return Optional.ofNullable(directory);
    }

    public TownshipSchedulingProblem prepareBenchmarkScheduling(TownshipSchedulingRequest townshipSchedulingRequest) {
        ProblemTransferProcess process = new ProblemTransferProcess(townshipSchedulingRequest);
        return process.buildProblem();
    }


    @Override
    public boolean checkWeatherReadyToSolve(String uuid) {
        if (Objects.isNull(uuid) || uuid.isBlank()) {
            return false;
        }

        return idProblemMap.containsKey(uuid);
    }

    @Override
    public void remove(String problemId) {
        unlink(problemId);
        if (townshipProblemEntityRepository.existsById(problemId)) {
            townshipProblemEntityRepository.deleteById(problemId);
        }
    }

    @Override
    public void unlink(String problemId) {
        SolverJob<TownshipSchedulingProblem> solverJob = this.idSolverJobMap.get(problemId);
        if (Objects.nonNull(solverJob)) {
            solverJob.terminateEarly();
            this.idSolverJobMap.remove(problemId, solverJob);
        }
        TownshipSchedulingProblem townshipSchedulingProblem = this.idProblemMap.remove(problemId);
        this.problemIdRequestMap.remove(townshipSchedulingProblem);
    }

    @Override
    public void persist(String problemId) {
        TownshipSchedulingProblem townshipSchedulingProblem = gatherProblem(problemId);
        persist(townshipSchedulingProblem);
    }

    @Override
    public TownshipSchedulingProblem gatherProblem(String problemId) {
        SolverStatus solverStatus = getProblemSolverStatus(problemId);
        TownshipSchedulingProblem townshipSchedulingProblem = getProblem(problemId);
        if (townshipSchedulingProblem != null) {
            townshipSchedulingProblem.setSolverStatus(solverStatus);
        }
        return townshipSchedulingProblem;
    }

    @Override
    public void persist(TownshipSchedulingProblem townshipSchedulingProblem) {
        ProblemPersistingPrecess problemPersistingPrecess = new ProblemPersistingPrecess(townshipSchedulingProblem);
        TownshipProblemEntity townshipProblemEntity = problemPersistingPrecess.process();
        transactionTemplate.executeWithoutResult(_ -> {
            townshipProblemEntityRepository.save(townshipProblemEntity);
        });
    }

    @Override
    public SolverStatus getProblemSolverStatus(String problemId) {
        return solverManager.getSolverStatus(problemId);
    }

    @Override
    public Optional<TownshipSchedulingProblem> load(String problemId) {
        if (this.townshipProblemEntityRepository.existsById(problemId)) {
            Optional<TownshipProblemEntity> townshipProblemEntityOptional = this.townshipProblemEntityRepository.findByUuid(problemId, TownshipProblemEntity.class);
            return townshipProblemEntityOptional.map(townshipProblemEntity -> {
                        return new ProblemExternalizedProcess(townshipProblemEntity.getProblemSerialized());
                    })
                    .map(ProblemExternalizedProcess::process)
                    .map(townshipSchedulingProblem -> this.idProblemMap.putIfAbsent(problemId, townshipSchedulingProblem));
        } else {
            return Optional.empty();
        }

    }

    @Override
    public Collection<TownshipSchedulingProblem> loadPersistedSchedulingProblem() {
        return this.townshipProblemEntityRepository.queryAll(TownshipProblemEntity.class)
                .stream()
                .map(ProblemExternalizedProcess::new)
                .map(ProblemExternalizedProcess::process)
                .peek(townshipSchedulingProblem -> this.idProblemMap.put(townshipSchedulingProblem.getUuid(), townshipSchedulingProblem))
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public Collection<TownshipSchedulingProblem> getLinkedSchedulingProblem() {
        return this.idProblemMap.values();
    }


}
