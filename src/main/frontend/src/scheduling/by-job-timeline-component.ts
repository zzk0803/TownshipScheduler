// import {DateTimeFormatter, Duration, LocalDateTime} from '@js-joda/core';
// import {html, LitElement, PropertyValues} from 'lit';
// import {customElement, property, state} from 'lit/decorators.js';
// import {DataGroup, DataItem} from 'vis-timeline';
// import {Job, Line, WorkCalendar} from "./type";
// import "./lit-vis-timeline";
//
// @customElement('by-job-timeline-component')
// export class ByJobTimelineComponent
//     extends LitElement {
//     // @property()
//     // pullScheduleResult?: PackagingSchedule;
//
//     @property()
//     lines: Array<Line> = new Array<Line>();
//
//     @property()
//     jobs: Array<Job> = new Array<Job>();
//
//     @property()
//     workCalendar?: WorkCalendar;
//
//     @state()
//     dataSetItems: DataItem[] = [];
//
//     @state()
//     dataGroupItems: DataGroup[] = [];
//
//     @state()
//     dateWindowStartString?: string;
//
//     @state()
//     dataWindowEndString?: string;
//
//
//     private formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//     render() {
//         return html`
//             <lit-vis-timeline
//                     .datasets="${this.dataSetItems}"
//                     .groups="${this.dataGroupItems}"
//                     .options="${{
//                         timeAxis: {scale: "hour"},
//                         orientation: {axis: "top"},
//                         stack: false,
//                         xss: {disabled: true}, // Items are XSS safe through JQuery
//                         zoomMin: 1000 * 60 * 60 * 12 // Half day in milliseconds
//                     }}"
//                     .fromDate="${this.dateWindowStartString}"
//                     .toDate="${this.dataWindowEndString}">
//             </lit-vis-timeline>
//         `;
//     }
//
//     protected willUpdate(_changedProperties: PropertyValues) {
//         if (_changedProperties.has("workCalendar")) {
//             this.dateWindowStartString = this.workCalendar?.fromDate;
//             this.dataWindowEndString = this.workCalendar?.toDate;
//             // this.dataWindowEndString = LocalDate.parse(this.workCalendar?.fromDate as string).plusDays(1).toString();
//         }
//
//         if (_changedProperties.has('jobs') || _changedProperties.has('lines')) {
//             const dataSetItems: DataItem[] = [];
//             const dataGroupItems: DataGroup[] = [];
//
//             this.jobs?.map((job) => {
//                 dataGroupItems.push({
//                     id: job.id,
//                     content: job.name
//                 });
//
//                 dataSetItems.push({
//                     id: job.id + "_readyToIdealEnd",
//                     content: "",
//                     group: job.id,
//                     start: job.minStartTime,
//                     end: job.idealEndTime,
//                     type: "background",
//                     style: "background-color: #8AE23433"
//                 });
//
//                 dataSetItems.push({
//                     id: job.id + "_idealEndToDue",
//                     content: "",
//                     group: job.id,
//                     start: job.idealEndTime,
//                     end: job.maxEndTime,
//                     type: "background",
//                     style: "background-color: #FCAF3E33"
//                 });
//
//                 if (job.line == null || job.startCleaningDateTime == null || job.startProductionDateTime == null || job.endDateTime == null) {
//                     dataSetItems.push({
//                         id: job.id,
//                         group: job.id,
//                         content: `
//                             <div>
//                                 <h5 class="card-title mb-1">Unassigned</h5>
//                             </div>
//                     `,
//                         start: job.minStartTime,
//                         end: LocalDateTime.parse(job.minStartTime, this.formatter)
//                             .plus(Duration.ofSeconds(job.duration))
//                             .toString(),
//                         style: "background-color: #EF292999"
//                     });
//                 } else {
//                     const beforeReady = LocalDateTime.parse(job.startProductionDateTime, this.formatter)
//                         .isBefore(LocalDateTime.parse(job.minStartTime, this.formatter));
//                     const afterDue = LocalDateTime.parse(job.endDateTime, this.formatter)
//                         .isAfter(LocalDateTime.parse(job.maxEndTime, this.formatter));
//
//
//                     dataSetItems.push({
//                         id: job.id + "_cleaning",
//                         group: job.id,
//                         content: "Cleaning",
//                         start: job.startCleaningDateTime,
//                         end: job.startProductionDateTime,
//                         style: "background-color: #FCAF3E99"
//                     });
//
//                     dataSetItems.push({
//                         id: job.id,
//                         group: job.id,
//                         content: `
//                             <div>
//                                 <p class="card-text">${job.line.name}</p>
//                             </div>
//                             ${beforeReady
//                             ? `<p class="badge badge-danger mb-0">Before ready (too early)</p>`
//                             : ``}
//                             ${afterDue ? `<p class="badge badge-danger mb-0">After due (too late)</p>` : ``}
//                         `,
//                         start: job.startProductionDateTime,
//                         end: job.endDateTime
//                     });
//                 }
//
//             });
//
//             this.dataSetItems = dataSetItems;
//             this.dataGroupItems = dataGroupItems;
//         }
//     }
// }
