import {DateTimeFormatter, LocalDateTime} from "@js-joda/core";
import {html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import {DataGroup, DataItem} from 'vis-timeline';
import {Job, Line, WorkCalendar} from "./type";
import "./lit-vis-timeline";

@customElement('by-line-timeline-component')
export class ByLineTimelineComponent
    extends LitElement {

    // @property()
    // packagingSchedule?: PackagingSchedule;

    @property()
    lines: Array<Line> = new Array<Line>();

    @property()
    jobs: Array<Job> = new Array<Job>();

    @property()
    workCalendar?: WorkCalendar;

    @state()
    dataSetItems: DataItem[] = [];

    @state()
    dataGroupItems: DataGroup[] = [];

    @state()
    dateWindowStartString?: string;

    @state()
    dataWindowEndString?: string;

    private formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    render() {

        return html`
            <lit-vis-timeline
                    .datasets="${this.dataSetItems}"
                    .groups="${this.dataGroupItems}"
                    .options="${{
                        timeAxis: {scale: "hour"},
                        orientation: {axis: "top"},
                        stack: false,
                        xss: {disabled: true}, // Items are XSS safe through JQuery
                        zoomMin: 1000 * 60 * 60 * 12 // Half day in milliseconds
                    }}"
                    .fromDate="${this.dateWindowStartString}"
                    .toDate="${this.dataWindowEndString}">
            </lit-vis-timeline>
        `;
    }

    protected willUpdate(_changedProperties: PropertyValues) {
        if (_changedProperties.has("workCalendar")) {
            this.dateWindowStartString = this.workCalendar?.fromDate;
            this.dataWindowEndString = this.workCalendar?.toDate;
        }

        if (_changedProperties.has('jobs') || _changedProperties.has('lines')) {
            const dataSetItems: DataItem[] = [];
            const dataGroupItems: DataGroup[] = [];

            this.lines?.map((item) => {
                // let groupCardEL = document.createElement("div");
                // groupCardEL.innerHTML = `
                //         <h5 class="card-title mb-1">${item.name}</h5>
                //         <p class="card-text ms-2 mb-0">${item.operator}</p>
                // `
                dataGroupItems.push({
                    id: item.id,
                    content: `
                        <h5 class="card-title mb-1">${item.name}</h5>
                        <p class="card-text ms-2 mb-0">${item.operator}</p>
                `
                });
            });

            this.jobs?.map((job) => {
                if (job.line == null || job.startCleaningDateTime == null || job.startProductionDateTime == null || job.endDateTime == null) {
                } else {

                    let beforeReady = LocalDateTime.parse(job.startProductionDateTime, this.formatter)
                        .isBefore(LocalDateTime.parse(job.minStartTime, this.formatter));
                    let afterDue = LocalDateTime.parse(job.endDateTime, this.formatter)
                        .isAfter(LocalDateTime.parse(job.maxEndTime, this.formatter));

                    // let lineJobEL = document.createElement("div");
                    // lineJobEL.innerHTML = `
                    //     <p class="card-text">${job.name}</p>
                    //     ${beforeReady ?? `<p class="badge badge-danger mb-0">Before ready (too early)</p>`}
                    //     ${afterDue ?? `<p class="badge badge-danger mb-0">After due (too late)</p>`}
                    // `

                    dataSetItems.push({
                        id: job.id + "_cleaning",
                        group: job.line.id,
                        content: "Cleaning",
                        start: job.startCleaningDateTime,
                        end: job.startProductionDateTime,
                        style: "background-color: #FCAF3E99"
                    });
                    dataSetItems.push({
                        id: job.id,
                        group: job.line.id,
                        content: `
                            <div> 
                                <p class="card-text">${job.name}</p>
                                ${beforeReady ? `<p class="badge badge-danger mb-0">Before ready (too early)</p>` : ``}
                                ${afterDue ? `<p class="badge badge-danger mb-0">After due (too late)</p>` : ``}
                            </div>
                    `,
                        start: job.startProductionDateTime,
                        end: job.endDateTime
                    });
                }
            });

            this.dataSetItems = dataSetItems;
            this.dataGroupItems = dataGroupItems;
        }

    }
}
