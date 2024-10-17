export declare type ScoreAnalysisFetchPolicy = "FETCH_ALL" | "FETCH_SHALLOW";

export declare type WorkCalendar = {
    fromDate: string;
    toDate: string;
}

export declare type Product = {
    id: string;
    name: string;
    cleaningDurations: Map<Product, number>
}

export declare type Line = {
    id: string;
    name: string;
    operator: number;
    startDateTime: Date;
    jobs: Array<Job>;
}

export declare type Job = {
    id: string;
    name: string;
    product: Product;
    duration: number;
    minStartTime: string;
    idealEndTime: string;
    maxEndTime: string;
    priority: number;
    pinned: boolean;
    line: Line;
    previousJob: Job;
    nextJob: Job;
    startCleaningDateTime: string;
    startProductionDateTime: string;
    endDateTime: string;
}

export declare type strHardMediumSoftLongScore = {
    initScore: number;
    hardScore: number;
    mediumScore: number;
    softScore: number;
}

export type SolverStatus = "SOLVING_SCHEDULED" | "SOLVING_ACTIVE" | "NOT_SOLVING";

export declare type PackagingSchedule = {
    workCalendar: WorkCalendar;
    products: Array<Product>;
    lines: Array<Line>;
    jobs: Array<Job>;
    score: HardMediumSoftLongScore;
    solverStatus: SolverStatus;
}
