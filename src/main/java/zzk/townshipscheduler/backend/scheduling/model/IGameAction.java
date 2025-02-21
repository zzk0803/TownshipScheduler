//package zzk.townshipscheduler.backend.scheduling.model;
//
//import lombok.Builder;
//import lombok.Data;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//public interface IGameAction {
//
//    String actionUuid();
//
//    List<Consequence> actionConsequence();
//
//    @Data
//    @Builder
//    class Consequence {
//
//        String actionUuid;
//
//        LocalDateTime playerArrangeDateTime;
//
//        LocalDateTime gameFinishedDateTime;
//
//        SchedulingProduct schedulingProduct;
//
//        Integer delta;
//
//    }
//
//}
