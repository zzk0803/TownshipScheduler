import {DateTimeFormatter, Duration} from '@js-joda/core';
import {html, LitElement} from 'lit';
import {customElement, property, query, state} from 'lit/decorators.js';
import './by-line-timeline-component';
import './by-job-timeline-component';
import {bootstrapStyles} from "./external-styles";
import {Job, Line, Product} from './type';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/vertical-layout';

export interface ISchedulingViewRemoteCall {

    // analyze(fetchPolicy: ScoreAnalysisFetchPolicy): ScoreAnalysis;

    stopSolving(): Promise<void>;

    packagingSchedule(): Promise<void>;

    solve(): Promise<void>;

}

@customElement('scheduling-view')
export class SchedulingView
    extends LitElement {

    static styles = [
        bootstrapStyles
    ]

    disconnectedCallback() {
        super.disconnectedCallback();
    }

    render() {
        return html`
            <div class="container-fluid">

                <div class="sticky-top d-flex justify-content-center align-items-center" aria-live="polite"
                     aria-atomic="true">
                    <div id="notificationPanel" style="position: absolute; top: .5rem;"></div>
                </div>

                <h1>Food packaging schedule solver</h1>
                <p>Generate the optimal schedule for your food packaging manufacturing lines.</p>

                <div class="mb-2">
                    <button id="refreshButton" type="button" class="btn btn-secondary" @click="${this.refreshSchedule}">
                        <span class="fas fa-refresh"></span> Refresh
                    </button>
                    ${
                            this.boolSolving
                                    ? html`
                                        <button id="stopSolvingButton" type="button"
                                                class="btn btn-danger" @click="${this.stopSolving}">
                                            <span class="fas fa-stop"></span> Stop solving
                                        </button>
                                    `
                                    : html`
                                        <button id="solveButton" type="button" class="btn btn-success"
                                                @click="${this.solve}">
                                            <span class="fas fa-play"></span> Solve
                                        </button>
                                    `
                    }

                    <span id="score" class="score ms-2 align-middle fw-bold">Score: ${this.score}</span>

                    <div class="float-end">
                        <ul class="nav nav-pills" role="tablist">
                            <li class="nav-item" role="presentation">
                                <button class="nav-link active" id="byLineTab" data-bs-toggle="tab"
                                        data-bs-target="#byLinePanel" type="button" role="tab"
                                        aria-controls="byLinePanel"
                                        aria-selected="true">By line
                                </button>
                            </li>
                            <li class="nav-item" role="presentation">
                                <button class="nav-link" id="byJobTab" data-bs-toggle="tab" data-bs-target="#byJobPanel"
                                        type="button" role="tab" aria-controls="byJobPanel" aria-selected="false">By job
                                </button>
                            </li>
                        </ul>
                    </div>
                </div>

                <div class="mb-4 tab-content">

                    <by-line-timeline-component .jobs="${this.jobs}" .lines="${this.lines}"
                                                .fromDate="${this.fromDate}" .toDate="${this.toDate}">
                    </by-line-timeline-component>

                    <hr/>

                    <by-job-timeline-component .jobs="${this.jobs}" .lines="${this.lines}"
                                               .fromDate="${this.fromDate}" .toDate="${this.toDate}">
                    </by-job-timeline-component>

                </div>

                <h2>Unassigned jobs</h2>
                <div id="unassignedJobs" class="row row-cols-3 g-3 mb-4">
                    ${this.jobs
                            ?.filter(job => job.line == null || job.startCleaningDateTime == null || job.startProductionDateTime == null || job.endDateTime == null)
                            ?.map(job => {
                                return html`
                                    <div class="col">
                                        <div class="card">
                                            <div class="card-body p-2">
                                                <h5 class="card-title mb-1">${job.name}</h5>
                                                <p class="card-text ms-2 mb-0">
                                                    ${Math.floor(Duration.ofSeconds(job.duration)
                                                            .toMinutes() / 60)} hours
                                                    ${Duration.ofSeconds(job.duration).toMinutes() % 60} mins
                                                </p>
                                                <p class="card-text ms-2 mb-0">
                                                    Min: ${job.minStartTime}
                                                </p>
                                                <p class="card-text ms-2 mb-0">
                                                    Ideal:
                                                    ${job.idealEndTime}
                                                </p>
                                                <p class="card-text ms-2 mb-0">
                                                    Max: ${job.maxEndTime}
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                `;
                            })
                    }
                </div>
            </div>
        `;
    }

    dateTimeFormatter = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss');

    @property()
    serverRefreshOnSolvingIntervalInSecond: string = "2s";

    @property()
    products: Product[] = [];

    @property()
    jobs: Job[] = [];

    @property()
    lines: Line[] = [];

    @property()
    fromDate: string = '';

    @property()
    toDate: string = '';

    //"SOLVING_SCHEDULED" | "SOLVING_ACTIVE" | "NOT_SOLVING";
    @property()
    solverStatus: string = '';

    @property()
    score: string = '';

    @query('#unassignedJobs')
    unassignedJobsDiv!: HTMLDivElement;

    @state()
    boolSolving: boolean = false;

    @state()
    unassignedJobsCount: number = 0;

    $server?: ISchedulingViewRemoteCall;

    async refreshSchedule() {
        await this.$server?.packagingSchedule();
    }

    async solve() {
        this.$server?.solve();
        this.refreshSolvingButtons(this.solverStatus);
    }

    refreshSolvingButtons(solverStatus: string) {
        if (solverStatus == "SOLVING_SCHEDULED" || solverStatus == "SOLVING_ACTIVE") {
            this.boolSolving = true;
        }else if (solverStatus=="NOT_SOLVING"){
            this.boolSolving = false;
        }else {
            console.error(solverStatus);
        }
    }

    async stopSolving() {
        this.$server?.stopSolving();
        this.refreshSolvingButtons(this.solverStatus);
    }
}

declare global {
    interface HTMLElementTagNameMap {
        'scheduling-view': SchedulingView;
    }
}

