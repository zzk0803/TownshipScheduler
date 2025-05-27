import {Duration} from "@js-joda/core";

export declare type ScoreAnalysisFetchPolicy = "FETCH_ALL" | "FETCH_SHALLOW";

export declare interface IVisTimeLineRemote{
    pullScheduleResult(): void;
}

export declare interface SchedulingWorkTimeLimit {

    startDateTime: string;

    endDateTime: string;

}

// export declare type Product = {
//     id: string;
//     name: string;
//     cleaningDurations: Map<Product, number>
// }

export declare interface SchedulingOrder {

    id: number,

    orderType: string,

    productAmountBill: Map<SchedulingProduct, number>,

    deadline: Date
}

export declare interface SchedulingProducingExecutionMode {

    id: number,

    product: SchedulingProduct,

    materials: Map<SchedulingProduct, number>,

    executeDuration: Duration
}

export declare interface SchedulingProduct {

    id: number

    name: string

    level: number

    requireFactory: SchedulingFactoryInfo

    executionModeSet: SchedulingProducingExecutionMode[]
}

export declare interface SchedulingFactoryInfo {

    id: number,

    categoryName: string,

    level: number,

    portfolioGoods: SchedulingProduct[],

    producingStructureType: SchedulingFactoryType,

    defaultInstanceAmount: number,

    defaultProducingCapacity: number,

    defaultReapWindowCapacity: number,

    maxProducingCapacity: number,

    maxReapWindowCapacity: number,

    maxInstanceAmount: number
}

export declare enum SchedulingFactoryType {
    QUEUE,
    SLOT
}

export declare interface SchedulingFactory {

    id: number,

    categoryName: string,

    info: SchedulingFactoryInfo,

    seqNum: number,

    producingLength: number,

    reapWindowSize: number
}

export declare interface BaseProducingArrangement {
    id: string,

    uuid: string,

    product: string,

    arrangeFactory: string,

    arrangeFactoryId: string,

    producingDuration: string,

    arrangeDateTime: string,

    gameProducingDateTime: string,

    gameCompletedDateTime: string
}

export declare interface SchedulingFactoryQueueProducingArrangement
    extends BaseProducingArrangement {

    nextQueueProducingArrangement: SchedulingFactoryQueueProducingArrangement

    planningPreviousProducingArrangementOrFactory: SchedulingFactoryQueueProducingArrangement | SchedulingFactory

}

export declare interface SchedulingFactorySlotProducingArrangement
    extends BaseProducingArrangement {

}

export declare type BendableScore = {
    score: string
}

export type SolverStatus = "SOLVING_SCHEDULED" | "SOLVING_ACTIVE" | "NOT_SOLVING";
