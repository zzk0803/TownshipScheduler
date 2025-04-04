//package zzk.townshipscheduler.ui.views.scheduling;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.vaadin.flow.component.*;
//import com.vaadin.flow.component.dependency.JsModule;
//import com.vaadin.flow.component.dependency.NpmPackage;
//import com.vaadin.flow.component.littemplate.LitTemplate;
//import com.vaadin.flow.router.PageTitle;
//import com.vaadin.flow.router.Route;
//import com.vaadin.flow.server.VaadinSession;
//import com.vaadin.flow.server.auth.AnonymousAllowed;
//import com.vaadin.flow.spring.annotation.SpringComponent;
//import com.vaadin.flow.spring.annotation.UIScope;
//import jakarta.annotation.Resource;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.TaskScheduler;
//import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.Objects;
//import java.util.UUID;
//import java.util.concurrent.ScheduledFuture;
//
//@Slf4j
//@PageTitle("Township Scheduling")
//@Tag("scheduling-view")
//@NpmPackage(value = "vis-timeline", version = "7.7.3")
//@NpmPackage(value = "@js-joda/core", version = "5.6.3")
//@JsModule("./src/scheduling/scheduling-view.ts")
//@JsModule("./src/scheduling/lit-vis-timeline.ts")
//@RequiredArgsConstructor
//@SpringComponent
//@UIScope
//@AnonymousAllowed
//public class LitSchedulingTemplatesView extends LitTemplate {
//
//    public static final String SESSION_SCHEDULING_SOLUTION_KEY = "schedulingViewRefreshTimerFuture";
//
//    private final ITownshipSchedulingService townshipSchedulingService;
//
//    private final TaskScheduler springTaskScheduler;
//
//    private final PropertyDescriptor<String, String> serverRefreshOnSolvingIntervalInSecond
//            = PropertyDescriptors.propertyWithDefault(
//            "serverRefreshOnSolvingIntervalInSecond",
//            "2s"
//    );
//
//    @Getter
//    @Setter
//    private SchedulingView schedulingView;
//
//    private ObjectMapper objectMapper;
//
//    @Value("${timefold.solver.termination.spent-limit}")
//    private String timefoldComputingDuration;
//
//    private UUID problemId;
//
//    @Override
//    protected void onAttach(AttachEvent attachEvent) {
//        super.onAttach(attachEvent);
//        pullScheduleResult();
//    }
//
//    @ClientCallable
//    public void pullScheduleResult() {
//        TownshipSchedulingProblem townshipSchedulingProblem = townshipSchedulingService.getSchedule(getProblemId());
//        getElement().setPropertyList(
//                "schedulingOrder",
//                new ArrayList<>(townshipSchedulingProblem.getSchedulingOrderSet())
//        );
//        getElement().setPropertyList(
//                "schedulingProduct",
//                new ArrayList<>(townshipSchedulingProblem.getSchedulingProductSet())
//        );
//        getElement().setPropertyList(
//                "schedulingFactory",
//                new ArrayList<>(townshipSchedulingProblem.getSchedulingFactoryInfoSet())
//        );
//        getElement().setPropertyList(
//                "schedulingFactorySlot",
//                new ArrayList<>(townshipSchedulingProblem.getSchedulingFactoryInstanceSet())
//        );
//        getElement().setPropertyList(
//                "schedulingGameAction",
//                new ArrayList<>(townshipSchedulingProblem.getSchedulingPlayerFactoryActions())
//        );
////        getElement().setProperty(
////                "schedulingGamePlayer",
////                json(townshipSchedulingProblem.getSchedulingWarehouse())
////        );
//
//        getElement().setProperty("solverStatus", townshipSchedulingProblem.getSolverStatus().name());
//        if (Objects.nonNull(townshipSchedulingProblem.getScore())) {
//            getElement().setProperty(
//                    "score",
//                    townshipSchedulingProblem.getScore().toString()
//            );
//        }
//
//    }
//
//    public UUID getProblemId() {
//        return problemId;
//    }
//
//    public void setProblemId(UUID problemId) {
//        this.problemId = problemId;
//    }
//
//    private String json(Object object) {
//        try {
//            return objectMapper.writeValueAsString(object);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @ClientCallable
//    public void stopSolving() {
//        townshipSchedulingService.abort(getProblemId());
//        try {
//            ScheduledFuture<?> scheduledFuture = (ScheduledFuture<?>) VaadinSession.getCurrent()
//                    .getAttribute(SESSION_SCHEDULING_SOLUTION_KEY);
//            scheduledFuture.cancel(true);
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
//    }
//
//    @ClientCallable
//    public void solve() {
//        townshipSchedulingService.scheduling(this.getProblemId());
//
//        if (VaadinSession.getCurrent().getAttribute(SESSION_SCHEDULING_SOLUTION_KEY) == null) {
//            try {
//                ScheduledFuture<?> scheduledFuture
//                        = springTaskScheduler.scheduleAtFixedRate(
//                        UI.getCurrent()
//                                .accessLater(
//                                        this::pullScheduleResult,
//                                        null
//                                ),
//                        Duration.parse(
//                                "PT" + getFlowRefreshOnSolvingIntervalInSecond().toUpperCase()
//                        )
//                );
//
//                springTaskScheduler.schedule(
//                        () -> scheduledFuture.cancel(true),
//                        Instant.now()
//                                .plus(Duration.parse("PT" + this.timefoldComputingDuration.toUpperCase()))
//                );
//
//                VaadinSession.getCurrent()
//                        .setAttribute(
//                                SESSION_SCHEDULING_SOLUTION_KEY,
//                                scheduledFuture
//                        );
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//
//    public String getFlowRefreshOnSolvingIntervalInSecond() {
//        return get(serverRefreshOnSolvingIntervalInSecond);
//    }
//
//    public void setFlowRefreshOnSolvingIntervalInSecond(String flowRefreshOnSolvingIntervalInSecond) {
//        set(this.serverRefreshOnSolvingIntervalInSecond, flowRefreshOnSolvingIntervalInSecond);
//    }
//
//    public ObjectMapper getObjectMapper() {
//        return objectMapper;
//    }
//
//    @Qualifier("java8EnhancedObjectMapper")
//    @Resource
//    public void setObjectMapper(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//    }
//
//}
