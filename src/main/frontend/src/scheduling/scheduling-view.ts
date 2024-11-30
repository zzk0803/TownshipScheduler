import {css, html, LitElement, PropertyValues} from 'lit';
import {customElement, property, query, state} from 'lit/decorators.js';
import './lit-vis-timeline';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/vertical-layout';
import {utility} from "@vaadin/vaadin-lumo-styles/utility";
import {DateTimeFormatter} from "@js-joda/core";
import {DataGroup, DataItem} from "vis-timeline";
import {
    SchedulingFactory,
    SchedulingFactorySlot,
    SchedulingGamePlayer,
    SchedulingOrder,
    SchedulingProducing,
    SchedulingProduct,
    WorkCalendar
} from "Frontend/src/scheduling/type";

export interface ISchedulingViewRemoteCall {

    // analyze(fetchPolicy: ScoreAnalysisFetchPolicy): ScoreAnalysis;

    stopSolving(): Promise<void>;

    pullScheduleResult(): Promise<void>;

    solve(): Promise<void>;

}

@customElement('scheduling-view')
export class SchedulingView
    extends LitElement {

    static styles = [
        utility,
        css`

        `
    ]

    protected willUpdate(_changedProperties: PropertyValues) {
        if (_changedProperties.has("workCalendar")) {
            this.dateWindowStartString = this.workCalendar?.fromDate;
            this.dataWindowEndString = this.workCalendar?.toDate;
        }

        if (_changedProperties.has('schedulingFactorySlot') || _changedProperties.has('schedulingProducing')) {
            const dataSetItems: DataItem[] = [];
            const dataGroupItems: DataGroup[] = [];

            this.schedulingFactorySlot?.map((slot) => {
                dataGroupItems.push({
                    id: slot.id,
                    content: ` <h6>${slot.schedulingFactory.category + "-" + slot.id}</h6>`
                });
            });

            this.schedulingProducing?.map((producing) => {
                if (producing.schedulingFactorySlot == null
                    || producing.arrangeDateTime == null || producing.producingInGameDateTime == null || producing.completedInGameDateTime == null
                ) {
                    this.unsignedProducing.push(producing);
                } else {
                    dataSetItems.push({
                        id: producing.uid + "_arrange",
                        group: producing.schedulingFactorySlot.id,
                        content: `<div>product ${producing.schedulingProduct.productName}</div>`,
                        start: producing.arrangeDateTime,
                        end: producing.arrangeDateTime
                    });

                    dataSetItems.push({
                        id: producing.uid + "_in_game_producing",
                        group: producing.schedulingFactorySlot.id,
                        content: `<div>${producing.schedulingProduct.productName} producing</div>`,
                        start: producing.producingInGameDateTime,
                        end: producing.completedInGameDateTime
                    });

                    dataSetItems.push({
                        id: producing.uid + "_in_game_completed",
                        group: producing.schedulingFactorySlot.id,
                        content: `<div>${producing.schedulingProduct.productName} completed</div>`,
                        start: producing.completedInGameDateTime,
                        end: producing.completedInGameDateTime
                    });

                }
            });

            this.dataSetItems = dataSetItems;
            this.dataGroupItems = dataGroupItems;
        }

    }

    @property()
    serverRefreshOnSolvingIntervalInSecond: string = "2s";

    @property()
    fromDate: string = '';

    @property()
    toDate: string = '';

    //"SOLVING_SCHEDULED" | "SOLVING_ACTIVE" | "NOT_SOLVING";
    @property()
    solverStatus: string = '';

    @state()
    score: string = '';

    @query('#unassignedJobs')
    unassignedJobsDiv!: HTMLDivElement;

    @state()
    boolSolving: boolean = false;

    @state()
    unassignedJobsCount: number = 0;

    @property()
    workCalendar?: WorkCalendar;

    @property()
    schedulingOrder: SchedulingOrder[] = [];

    @property()
    schedulingProduct: SchedulingProduct[] = [];

    @property()
    schedulingFactory: SchedulingFactory[] = [];

    @property()
    schedulingFactorySlot: SchedulingFactorySlot[] = [];

    @property()
    schedulingProducing: SchedulingProducing[] = [];

    @state()
    unsignedProducing: SchedulingProducing[] = [];

    @property()
    schedulingGamePlayer!: SchedulingGamePlayer;

    @state()
    dataSetItems: DataItem[] = [];

    @state()
    dataGroupItems: DataGroup[] = [];

    @state()
    dateWindowStartString?: string;

    @state()
    dataWindowEndString?: string;

    $server?: ISchedulingViewRemoteCall;

    private formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    render() {
        return html`
            <div class="flex flex-col flex-auto max-h-screen">
                <h1>Township Scheduling Solver</h1>
                <div >
                    <button id="refreshButton" type="button" class="btn btn-secondary" @click="${this.refreshSchedule}">
                        <span class="fas fa-refresh"></span> Refresh
                    </button>
                    ${
                            this.boolSolving
                                    ? html`
                                        <button id="stopSolvingButton" type="button"
                                                class="bg-error" @click="${this.stopSolving}">
                                            <span class="fas fa-stop"></span> Stop solving
                                        </button>
                                    `
                                    : html`
                                        <button id="solveButton" type="button" class="bg-success"
                                                @click="${this.solve}">
                                            <span class="fas fa-play"></span> Solve
                                        </button>
                                    `
                    }
                    <span id="score" class="score ms-m self-center">Score: ${this.score}</span>
                </div>

                <div class=" flex flex-col flex-grow">
                    <lit-vis-timeline
                            .datasets="${this.dataSetItems}"
                            .groups="${this.dataGroupItems}"
                    </lit-vis-timeline>
                </div>

                <div class=" grid grid-cols-4">
                    <fieldset>
                        <legend>unassign producing</legend>
                        ${this.unsignedProducing.map(producing => html`
                            <div class="border border-contrast-50">
                                <div>uid:${producing.uid}</div>
                                <div>productName:${producing.schedulingProduct.productName}</div>
                                <div>category:${producing.schedulingProduct.category}</div>
                            </div>
                        `)}
                    </fieldset>
                </div>
            </div>
        `;
    }

    async refreshSchedule() {
        await this.$server?.pullScheduleResult();
    }

    async solve() {
        this.$server?.solve();
        this.refreshSolvingButtons(this.solverStatus);
    }

    refreshSolvingButtons(solverStatus: string) {
        if (solverStatus == "SOLVING_SCHEDULED" || solverStatus == "SOLVING_ACTIVE") {
            this.boolSolving = true;
        } else if (solverStatus == "NOT_SOLVING") {
            this.boolSolving = false;
        } else {
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
