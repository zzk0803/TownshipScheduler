export declare type ScoreAnalysisFetchPolicy = "FETCH_ALL" | "FETCH_SHALLOW";

export declare type WorkCalendar = {
    fromDate: string;
    toDate: string;
}

// export declare type Product = {
//     id: string;
//     name: string;
//     cleaningDurations: Map<Product, number>
// }

export declare type SchedulingOrder = {
    id: string,
    entityId: number,
    orderType: string,
    itemAmountMap: Map<SchedulingProduct, number>,
    deadline: Date
}

export declare type SchedulingProduct = {
    productId: string
    productName: string
    category: string
    level: string
    cost: string
    sellPrice: string
    xp: string
    dealerValue: string
    helpValue: string
    bomString: string
    durationString: string
}

export declare type SchedulingFactory = {
    category: string,
    portfolioGoods: SchedulingProduct[],
    slotAmount: number,
    factorySlotList: SchedulingFactorySlot[]
}

export declare type SchedulingGamePlayer = {
    playerName: string,
    playerLevel: string
}


export declare type SchedulingFactorySlot = {
    id: number,
    schedulingFactory: SchedulingFactory,
    player: SchedulingGamePlayer,
    schedulingProducingList: SchedulingProducing[]
}

// export declare type Line = {
//     id: string;
//     name: string;
//     operator: number;
//     startDateTime: Date;
//     jobs: Array<Job>;
// }

export declare type SchedulingProducing = {
    uid: string,
    schedulingProduct:SchedulingProduct,
    schedulingFactorySlot: SchedulingFactorySlot,
    producingIndex: number,
    previousProducing: SchedulingProducing,
    nextProducing: SchedulingProducing,
    arrangeDateTime: Date,
    producingInGameDateTime: Date,
    completedInGameDateTime: Date
}

// export declare type Job = {
//     id: string;
//     name: string;
//     product: Product;
//     duration: number;
//     minStartTime: string;
//     idealEndTime: string;
//     maxEndTime: string;
//     priority: number;
//     pinned: boolean;
//     line: Line;
//     previousJob: Job;
//     nextJob: Job;
//     startCleaningDateTime: string;
//     startProductionDateTime: string;
//     endDateTime: string;
// }

export declare type HardMediumSoftLongScore = {
    initScore: number;
    hardScore: number;
    mediumScore: number;
    softScore: number;
}

export type SolverStatus = "SOLVING_SCHEDULED" | "SOLVING_ACTIVE" | "NOT_SOLVING";

// export declare type PackagingSchedule = {
//     workCalendar: WorkCalendar;
//     products: Array<Product>;
//     lines: Array<Line>;
//     jobs: Array<Job>;
//     score: HardMediumSoftLongScore;
//     solverStatus: SolverStatus;
// }

export declare type TownshipSchedulingProblem = {
    uid: string,
    schedulingGamePlayer: SchedulingGamePlayer,
    schedulingProductList: SchedulingProduct[],
    schedulingOrderList: SchedulingOrder,
    schedulingFactoryList: SchedulingFactory[],
    schedulingFactorySlotList: SchedulingFactorySlot[],
    schedulingProducingList: SchedulingProducing[],
    score: HardMediumSoftLongScore
}
