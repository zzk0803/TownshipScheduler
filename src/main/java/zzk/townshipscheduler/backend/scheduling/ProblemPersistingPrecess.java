package zzk.townshipscheduler.backend.scheduling;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import zzk.townshipscheduler.backend.dao.FieldFactoryEntityRepository;
import zzk.townshipscheduler.backend.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.dao.PlayerEntityRepository;
import zzk.townshipscheduler.backend.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.TownshipArrangementEntity;
import zzk.townshipscheduler.backend.persistence.TownshipProblemEntity;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

class ProblemPersistingPrecess {

    private final TownshipSchedulingProblem townshipSchedulingProblem;

    private final PlayerEntityRepository playerEntityRepository;

    private final OrderEntityRepository orderEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final FieldFactoryEntityRepository fieldFactoryEntityRepository;

    public ProblemPersistingPrecess(
            TownshipSchedulingProblem townshipSchedulingProblem,
            PlayerEntityRepository playerEntityRepository,
            OrderEntityRepository orderEntityRepository,
            ProductEntityRepository productEntityRepository,
            FieldFactoryEntityRepository fieldFactoryEntityRepository
    ) {
        this.townshipSchedulingProblem = townshipSchedulingProblem;
        this.playerEntityRepository = playerEntityRepository;
        this.orderEntityRepository = orderEntityRepository;
        this.productEntityRepository = productEntityRepository;
        this.fieldFactoryEntityRepository = fieldFactoryEntityRepository;
    }

    public TownshipProblemEntity process() {
        TownshipProblemEntity.TownshipProblemEntityBuilder entityBuilder = TownshipProblemEntity.builder();
        entityBuilder.uuid(this.townshipSchedulingProblem.getUuid());

        SchedulingWorkCalendar schedulingWorkCalendar = this.townshipSchedulingProblem.getSchedulingWorkCalendar();
        int minute = this.townshipSchedulingProblem.getDateTimeSlotSize()
                .getMinute();
        entityBuilder.workCalendarStart(schedulingWorkCalendar.getStartDateTime());
        entityBuilder.workCalendarEnd(schedulingWorkCalendar.getEndDateTime());
        entityBuilder.dateTimeSlotSizeInMinute(minute);

        SchedulingPlayer schedulingPlayer = this.townshipSchedulingProblem.getSchedulingPlayer();
        LocalTime sleepStart = schedulingPlayer.getSleepStart();
        LocalTime sleepEnd = schedulingPlayer.getSleepEnd();
        entityBuilder.player(
                playerEntityRepository.getReferenceById(
                        Long.valueOf(schedulingPlayer.getId())
                )
        );
        entityBuilder.sleepStartPickerValue(sleepStart);
        entityBuilder.sleepEndPickerValue(sleepEnd);

        List<SchedulingOrder> schedulingOrderList = this.townshipSchedulingProblem.getSchedulingOrderList();
        HashSet<OrderEntity> orderEntities = schedulingOrderList.stream()
                .map(SchedulingOrder::getId)
                .map(orderEntityRepository::getReferenceById)
                .collect(Collectors.toCollection(HashSet::new))
                ;
        entityBuilder.orderEntitySet(orderEntities);

        List<SchedulingProducingArrangement> schedulingProducingArrangementList
                = this.townshipSchedulingProblem.getSchedulingProducingArrangementList();
        HashSet<TownshipArrangementEntity> arrangementEntities = schedulingProducingArrangementList.stream()
                .map(this::mapArrangementPersisted)
                .collect(Collectors.toCollection(HashSet::new))
                ;
        //associate use self-defined setter
        //entityBuilder.townshipArrangementEntitySet(arrangementEntities);

        HardMediumSoftLongScore score = this.townshipSchedulingProblem.getScore();
        entityBuilder.scoreReadable(score.toString());

        TownshipProblemEntity townshipProblemEntity = entityBuilder.build();
        townshipProblemEntity.setTownshipArrangementEntitySet(arrangementEntities);

        return townshipProblemEntity;
    }

    private TownshipArrangementEntity mapArrangementPersisted(SchedulingProducingArrangement schedulingProducingArrangement) {
        TownshipArrangementEntity.TownshipArrangementEntityBuilder builder = TownshipArrangementEntity.builder();
        String uuid = schedulingProducingArrangement.getUuid();
        builder.uuid(uuid);

        SchedulingOrder schedulingOrder = schedulingProducingArrangement.getSchedulingOrder();
        builder.order(orderEntityRepository.getReferenceById(schedulingOrder.getId()));

        SchedulingProduct orderProduct = schedulingProducingArrangement.getSchedulingOrderProduct();
        SchedulingProduct currentProduct = schedulingProducingArrangement.getSchedulingProduct();
        SchedulingProduct targetProduct = (SchedulingProduct) schedulingProducingArrangement.getTargetActionObject();
        builder.currentProduct(productEntityRepository.getReferenceById(currentProduct.longIdentity()))
                .targetProduct(productEntityRepository.getReferenceById(targetProduct.longIdentity()))
                .orderProduct(productEntityRepository.getReferenceById(orderProduct.longIdentity()))
        ;

        LocalDateTime arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
        SchedulingFactoryInstance planningFactoryInstance = schedulingProducingArrangement.getPlanningFactoryInstance();
        builder.assignedDateTime(arrangeDateTime)
                .assignedFactoryInstance(fieldFactoryEntityRepository.getReferenceById(
                                planningFactoryInstance.getFieldFactoryId()
                        )
                );

        LocalDateTime producingDateTime = schedulingProducingArrangement.getProducingDateTime();
        LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
        builder.shadowProducingDateTime(producingDateTime)
                .shadowCompletedDateTime(completedDateTime);

        return builder.build();
    }

}
