import {css, html, LitElement, PropertyValueMap, PropertyValues} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import '@vaadin/vertical-layout';
import {vaadinStyles, visStyles} from './external-styles';
import {DataGroup, DataItem} from 'vis-timeline';
import {
    SchedulingFactoryInstance,
    SchedulingOrder,
    SchedulingProducingArrangement,
    SchedulingWorkCalendar
} from './type';


@customElement('by-factory-timeline-components')
export class ByFactoryTimelineComponents
    extends LitElement {

    static styles = [
        vaadinStyles,
        visStyles,
        css`

        `
    ];

    @property()
    schedulingWorkCalendar!: SchedulingWorkCalendar;

    @property()
    schedulingFactoryInstances: Array<SchedulingFactoryInstance> = new Array<SchedulingFactoryInstance>();

    @property()
    schedulingProducingArrangements: Array<SchedulingProducingArrangement> = new Array<SchedulingProducingArrangement>();

    @property()
    schedulingOrders: Array<SchedulingOrder> = new Array<SchedulingOrder>();

    @property()
    dataItems: DataItem[] = [];

    @property()
    groups: DataGroup[] = [];

    @property()
    dateWindowStartString?: string;

    @property()
    dateWindowEndString?: string;

    @property()
    dateTimeSlotSizeInMinute: number = 30;

    connectedCallback() {
        super.connectedCallback();
        this.classList.add('flex', 'flex-col', 'w-full', 'h-auto');
    }


    firstUpdated(_changedProperties: PropertyValues) {
        this.timelinePropertiesLitUpdate(_changedProperties);
    }

    willUpdate(_changedProperties: PropertyValues) {
        this.timelinePropertiesLitUpdate(_changedProperties);
    }

    render() {
        return html`
            <lit-vis-timeline
                    .datasets="${this.dataItems}"
                    .groups="${this.groups}"
                    .options="${{
                        timeAxis: {scale: 'minute', step: this.dateTimeSlotSizeInMinute},
                        orientation: {axis: 'top'},
                        zoomKey: 'ctrlKey',
                        stackSubgroups: false,
                        stack: false,
                        tooltip: {
                            followMouse: true,
                            template: function (originalItemData: any, parsedItemData: any) {
                                let start = originalItemData.start;
                                let resultTemplate = `
                                        <div style="display: flex;flex-direction: column;border: 1px solid black">
                                            <div style="display: flex">Arrange:${start}</div>
                                        </div>
                                        `;
                                if (originalItemData.end) {
                                    let end = originalItemData.end;
                                    resultTemplate =
                                            `
                                        <div style="display: flex;flex-direction: column;border: 1px solid black">
                                            <div style="display: flex">Producing:${start}</div>
                                            <div style="display: flex">Completed:${end}</div>
                                        </div>
                                    `;
                                }
                                return resultTemplate;
                            }
                        }
                    }}"
                    .fromDateTime="${this.dateWindowStartString}"
                    .toDateTime="${this.dateWindowEndString}">
            </lit-vis-timeline>
        `;
    }

    private timelinePropertiesLitUpdate(_changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>) {
        const dataGroupItems: DataGroup[] = [];
        const dataSetItems: DataItem[] = [];

        if (_changedProperties.has('schedulingWorkCalendar')) {
            this.dateWindowStartString = this.schedulingWorkCalendar?.startDateTime;
            this.dateWindowEndString = this.schedulingWorkCalendar?.endDateTime;
            dataSetItems.push({
                content: '',
                start: this.dateWindowStartString,
                end: this.dateWindowEndString,
                type: 'background',
                className: "calendar"
            });
        }

        if (_changedProperties.has('schedulingOrders')) {
            //do nothing
        }

        if (_changedProperties.has('schedulingFactoryInstances')) {
            this.schedulingFactoryInstances
                ?.map((factory) => {
                    dataGroupItems.push({
                        id: factory?.factoryReadableIdentifier,
                        content: ` 
                             <h6 class="mb-m">
                                ${(factory?.categoryName ?? 'Unknown Factory') + '#' + factory?.seqNum + '(' + factory.producingLength + ")"}
                            </h6>
                        `
                    });
                });

            this.groups = dataGroupItems;
        }

        if (_changedProperties.has('schedulingProducingArrangements')) {
            this.schedulingProducingArrangements
                ?.filter(arrangement => {
                    return arrangement.factoryReadableIdentifier != null && arrangement.arrangeDateTime != null;
                })
                ?.map(
                    (arrangement) => {
                        let arrangeDateTime = arrangement?.arrangeDateTime;
                        let producingDateTime = arrangement?.producingDateTime;
                        let completedDateTime = arrangement?.completedDateTime;
                        dataSetItems.push({
                            id: arrangement?.uuid + '_arrange',
                            className: 'arrange',
                            group: arrangement?.factoryReadableIdentifier,
                            content: `<p class="h-auto w-auto text-left">&nbsp;</p>`,
                            start: arrangeDateTime,
                            end: producingDateTime,
                            type: 'range',
                            subgroup: arrangement?.uuid
                        });

                        dataSetItems.push({
                            id: arrangement?.uuid + '_in_game',
                            group: arrangement?.factoryReadableIdentifier,
                            content: `<p class="h-auto w-auto text-center text-2xs"> ${arrangement?.product + "#" + arrangement.id}</p>`,
                            start: producingDateTime,
                            end: completedDateTime,
                            type: 'range',
                            subgroup: arrangement?.uuid
                        });
                    });

            this.dataItems = dataSetItems;
        }
    }
}

declare global {
    interface HTMLElementTagNameMap {
        'by-factory-timeline-components': ByFactoryTimelineComponents;
    }
}
