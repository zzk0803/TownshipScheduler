import {css, html, LitElement, PropertyValues} from 'lit';
import {customElement, property, query} from 'lit/decorators.js';
import '@vaadin/vertical-layout';
import {vaadinStyles, visStyles} from "./external-styles";
import {DataGroup, DataItem} from "vis-timeline";
import {BaseProducingArrangement, SchedulingFactory, SchedulingWorkTimeLimit} from "./type";


@customElement('by-factory-timeline-components')
export class ByFactoryTimelineComponents
    extends LitElement {

    static styles = [
        vaadinStyles,
        visStyles,
        css`

        `
    ]

    connectedCallback() {
        super.connectedCallback();
        this.classList.add("flex", "flex-col", "w-full", "h-auto");
    }

    willUpdate(_changedProperties: PropertyValues) {
        if (_changedProperties.has("schedulingWorkTimeLimit")) {
            this.dateWindowStartString = this.schedulingWorkDateTimeLimit?.startDateTime;
            this.dateWindowEndString = this.schedulingWorkDateTimeLimit?.endDateTime;
        }

        if (_changedProperties.has('schedulingFactory')) {
            const dataGroupItems: DataGroup[] = [];

            this.schedulingFactory?.map((factory) => {
                dataGroupItems.push({
                    id: factory?.categoryName + "#" + factory?.seqNum,
                    content: ` 
                         <h6 class="mb-m">
                            ${(factory?.categoryName ?? "Unknown Factory") + "#" + factory?.seqNum}
                        </h6>
                    `
                });
            });

            this.groups = dataGroupItems;
        }

        if (_changedProperties.has('producingArrangements')) {
            console.log(this.producingArrangements);

            const dataSetItems: DataItem[] = [];
            this.producingArrangements
                ?.filter(arrangement => {
                    return arrangement.schedulingFactory != null && arrangement.arrangeDateTime != null
                })
                ?.map(
                    (arrangement) => {
                        dataSetItems.push({
                            id: arrangement?.uuid + "_arrange",
                            group: arrangement?.schedulingFactory?.categoryName + "#" + arrangement?.schedulingFactory?.seqNum,
                            content: `<p class="h-auto w-auto text-center">arrange: ${arrangement?.schedulingProduct?.name}</p>`,
                            start: arrangement?.arrangeDateTime,
                            end: arrangement?.arrangeDateTime
                        });

                        dataSetItems.push({
                            id: arrangement?.uuid + "_in_game",
                            group: arrangement?.schedulingFactory?.categoryName + "#" + arrangement?.schedulingFactory?.seqNum,
                            content: `<p class="h-auto w-auto text-center">in game: ${arrangement?.schedulingProduct?.name}</p>`,
                            start: arrangement?.producingDateTime,
                            end: arrangement?.completedDateTime
                        });
                    })

            this.dataItems = dataSetItems;
        }
    }

    render() {
        return html`
            <lit-vis-timeline
                    .datasets="${this.dataItems}"
                    .groups="${this.groups}"
                    .options="${{
                        timeAxis: {scale: "hour"},
                        orientation: {axis: "top"},
                        zoomMin: 1000 * 60 * 60 * 12 // Half day in milliseconds
                    }}"
                    .fromDateTime="${this.dateWindowStartString}"
                    .toDateTime="${this.dateWindowEndString}">
            </lit-vis-timeline>
        `;
    }

    @query('#unassignedJobs')
    unassignedJobsDiv!: HTMLDivElement;

    @property()
    schedulingWorkDateTimeLimit!: SchedulingWorkTimeLimit;

    @property()
    schedulingFactory: Array<SchedulingFactory> = new Array<SchedulingFactory>();

    @property()
    producingArrangements: Array<BaseProducingArrangement> = new Array<BaseProducingArrangement>();

    @property()
    dataItems: DataItem[] = [];

    @property()
    groups: DataGroup[] = [];

    @property()
    dateWindowStartString?: string;

    @property()
    dateWindowEndString?: string;
}

declare global {
    interface HTMLElementTagNameMap {
        'by-factory-timeline-components': ByFactoryTimelineComponents;
    }
}
