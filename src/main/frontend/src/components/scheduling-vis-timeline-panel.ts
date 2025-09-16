import {css, html, LitElement} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/vertical-layout';
import {
    IVisTimeLineRemote,
    SchedulingFactoryInstance,
    SchedulingOrder,
    SchedulingProducingArrangement,
    SchedulingProduct,
    SchedulingWorkCalendar
} from "./type";
import {vaadinStyles, visStyles} from "./external-styles";
import {TabsSelectedChangedEvent} from "@vaadin/react-components";

@customElement('scheduling-vis-timeline-panel')
export class SchedulingVisTimelinePanel
    extends LitElement {

    static styles = [
        vaadinStyles,
        visStyles,
        css`

        `
    ]

    @property()
    schedulingWorkCalendar?: SchedulingWorkCalendar;

    @property()
    schedulingOrders: Array<SchedulingOrder> = new Array<SchedulingOrder>();

    @property()
    schedulingProducts: Array<SchedulingProduct> = new Array<SchedulingProduct>();

    @property()
    schedulingFactoryInstances: Array<SchedulingFactoryInstance> = new Array<SchedulingFactoryInstance>();

    @property()
    schedulingProducingArrangements: Array<SchedulingProducingArrangement> = new Array<SchedulingProducingArrangement>();

    @property()
    dateTimeSlotSizeInMinute: number = 30;

    @state()
    tabSelected: number = 0;

    $server!: IVisTimeLineRemote;

    connectedCallback() {
        super.connectedCallback();
        this.classList.add("flex", "flex-col", "w-full", "h-auto");
    }

    render() {
        return html`
            <vaadin-tabs slot="tabs" id="scheduling-view-tabs"
                         @selected-changed=${this.selectedChanged}>
                <vaadin-tab selected id="by-factory-tab">By Factory</vaadin-tab>
                <vaadin-tab id="by-order-tab">By Order</vaadin-tab>
            </vaadin-tabs>
            ${this.renderTimeline()}
        `;
    }

    async selectedChanged(e: TabsSelectedChangedEvent) {
        this.tabSelected = e.detail.value;
    }

    renderTimeline() {
        if (this.tabSelected == 0) {
            return this.renderFactoryTimeline();
        } else if (this.tabSelected == 1) {
            return this.renderOrderTimeline();
        } else {
            return this.renderFactoryTimeline();
        }

    }

    renderFactoryTimeline() {
        return html`
            <by-factory-timeline-components
                    .schedulingFactoryInstances="${this.schedulingFactoryInstances}"
                    .schedulingOrders="${this.schedulingOrders}"
                    .schedulingProducingArrangements="${this.schedulingProducingArrangements}"
                    .schedulingWorkCalendar="${this.schedulingWorkCalendar}"
                    .dateTimeSlotSizeInMinute="${this.dateTimeSlotSizeInMinute}"
            >
            </by-factory-timeline-components>
        `
    }

    renderOrderTimeline() {
        return html`
            <by-order-timeline-components
                    .schedulingFactoryInstances="${this.schedulingFactoryInstances}"
                    .schedulingOrders="${this.schedulingOrders}"
                    .schedulingProducingArrangements="${this.schedulingProducingArrangements}"
                    .schedulingWorkCalendar="${this.schedulingWorkCalendar}"
                    .dateTimeSlotSizeInMinute="${this.dateTimeSlotSizeInMinute}"
            >
            </by-order-timeline-components>
        `
    }
}

declare global {
    interface HTMLElementTagNameMap {
        'scheduling-vis-timeline-panel': SchedulingVisTimelinePanel;
    }
}
