export declare type ScoreAnalysisFetchPolicy = "FETCH_ALL" | "FETCH_SHALLOW";

export declare interface IVisTimeLineRemote {
    pullScheduleResult(): void;
}

export declare interface SchedulingWorkCalendar {

    startDateTime: string;

    endDateTime: string;

}

// export declare type Product = {
//     id: string;
//     name: string;
//     cleaningDurations: Map<Product, number>
// }

export declare interface SchedulingOrder {

    id: string,

    orderType: string,

    deadline: string
}

export declare interface SchedulingProduct {

    id: string,

    name: string,

    level: string,

    requireFactory: string

}

export declare interface SchedulingFactoryInfo {

    id: number,

    categoryName: string,

    level: number,

    portfolio: SchedulingProduct[],

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

export declare interface SchedulingFactoryInstance {

    id: number,

    categoryName: string,

    schedulingFactoryInfo: SchedulingFactoryInfo,

    factoryReadableIdentifier: string,

    seqNum: number,

    producingLength: number,

    reapWindowSize: number
}

export declare interface SchedulingProducingArrangement {

    id: string,

    uuid: string,

    order: string,

    product: string,

    factoryReadableIdentifier: string,

    producingDuration: string,

    arrangeDateTime: string,

    producingDateTime: string,

    completedDateTime: string

}

export declare type BendableScore = {
    score: string
}

export type SolverStatus = "SOLVING_SCHEDULED" | "SOLVING_ACTIVE" | "NOT_SOLVING";

export type DateTimeSlotSize = "TEN_MINUTES" | "HALF_HOUR" | "HOUR" | "TWO_HOUR" | "THREE_HOUR";
