import {css, html, LitElement, PropertyValueMap, PropertyValues} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import '@vaadin/vertical-layout';
import {vaadinStyles, visStyles, visTownshipStyles} from './external-styles';
import {DataGroup, DataItem} from 'vis-timeline';
import {
    SchedulingFactoryInstance,
    SchedulingOrder,
    SchedulingProducingArrangement,
    SchedulingProducingArrangementUnitGroup,
    SchedulingWorkCalendar
} from './type';

@customElement('by-unit-timeline-components')
export class ByUnitTimelineComponents
    extends LitElement {
    static styles = [
        vaadinStyles,
        visStyles,
        visTownshipStyles,
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
    schedulingProducingArrangementUnitGroups: Array<SchedulingProducingArrangementUnitGroup> = new Array<SchedulingProducingArrangementUnitGroup>();

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

    willUpdate(_changedProperties: PropertyValues) {
        this.timelinePropertiesLitUpdate(_changedProperties);
    }

    private timelinePropertiesLitUpdate(_changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>) {
        const dataGroupItems: DataGroup[] = [];
        const dataSetItems: DataItem[] = [];

        if (_changedProperties.has('schedulingWorkCalendar')) {
            this.dateWindowStartString = this.schedulingWorkCalendar?.startDateTime;
            this.dateWindowEndString = this.schedulingWorkCalendar?.endDateTime;
        }

        if (_changedProperties.has('schedulingFactoryInstances')) {
            //do nothing
        }

        if (_changedProperties.has('schedulingOrders')) {
            //do nothing
        }

        if (_changedProperties.has("schedulingProducingArrangementUnitGroups")) {
            this.schedulingProducingArrangementUnitGroups
                .forEach(unitGroup => {
                        let orderId = unitGroup.orderId;
                        let orderType = unitGroup.orderType;
                        let nestedOrderProductList = unitGroup.nestedOrderProductList;

                        nestedOrderProductList.forEach(orderProductPair => {
                            let orderProduct = orderProductPair.arrangementOrderProductName;
                            let orderProductArrangeId = orderProductPair.arrangementOrderProductArrangeId;

                            let unit = `${orderId}::${orderProduct}#${orderProductArrangeId}`;
                            dataGroupItems.push({
                                id: unit,
                                content: ` 
                                 <h6 class="mb-m">
                                     ${orderType + "#" + orderId + "::" + orderProduct + "#" + orderProductArrangeId}
                                </h6>
                            `
                            });

                        });

                        dataGroupItems.push({
                            id: orderId,
                            content: ` 
                             <h6 class="mb-m">
                                ${orderType + "#" + orderId}
                            </h6>
                        `,
                            nestedGroups: [...([...nestedOrderProductList]?.map(
                                orderProduct =>
                                    `${orderId}::${orderProduct.arrangementOrderProductName}#${orderProduct.arrangementOrderProductArrangeId}`
                            ))]
                        });
                    }
                );

            this.groups = dataGroupItems;
        }

        if (_changedProperties.has('schedulingProducingArrangements')) {

            this.schedulingProducingArrangements
                ?.filter(arrangement => {
                    return arrangement.factoryReadableIdentifier != null && arrangement.arrangeDateTime != null;
                })
                ?.map(
                    (arrangement) => {
                        let groupId = arrangement.order + "::" + arrangement.orderProduct + "#" + arrangement.orderProductArrangementId;
                        let subgroup = arrangement?.factoryReadableIdentifier + "#" + arrangement?.uuid;
                        dataSetItems.push({
                            id: arrangement?.uuid + '_arrange',
                            className: 'arrange',
                            group: groupId,
                            content: `<p class="h-auto w-auto text-left">&nbsp;</p>`,
                            type: 'range',
                            start: arrangement?.arrangeDateTime,
                            end: arrangement?.producingDateTime,
                            subgroup: subgroup
                        });

                        dataSetItems.push({
                            id: arrangement?.uuid + '_in_game',
                            group: groupId,
                            content: `<p class="h-auto w-auto text-center text-2xs">${arrangement.factoryReadableIdentifier + "#" + arrangement.product}</p>`,
                            type: 'range',
                            start: arrangement?.producingDateTime,
                            end: arrangement?.completedDateTime,
                            subgroup: subgroup
                        });
                    });

            this.dataItems = dataSetItems;
        }

    }

}

declare global {
    interface HTMLElementTagNameMap {
        'by-unit-timeline-components': ByUnitTimelineComponents;
    }
}
