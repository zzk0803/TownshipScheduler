import {css, html, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/vertical-layout';
import {
    BaseProducingArrangement,
    SchedulingFactory,
    SchedulingOrder,
    SchedulingProduct,
    SchedulingWorkTimeLimit
} from "./type";
import {vaadinStyles, visStyles} from "./external-styles";

@customElement('scheduling-vis-timeline-panel')
export class SchedulingVisTimelinePanel
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

    render() {
        /*
                    <vaadin-details class="flex flex-wrap w-full h-auto" summary="Unsigned Arrangement">
                <div class="flex flex-row flex-wrap m-xs p-xs">
                    ${
                            this.producingArrangements
                                    .filter(arrangement => {
                                       return  arrangement.arrangeFactory === "N/A" || arrangement.arrangeDateTime === "N/A";
                                    })
                                    .map(arrangement => {
                                        return html`
                                            <div class="flex flex-col border shadow-xs text-l font-semibold leading-s text-left m-m p-m">
                                                <span class="text-primary">Item:${arrangement?.product}</span>
                                                <span class="text-secondary">Duration:${arrangement?.producingDuration}</span>
                                            </div>
                                        `
                                    })
                    }
                </div>
            </vaadin-details>
         */
        return html`
            <by-factory-timeline-components
                    .schedulingFactory="${this.schedulingFactory}"
                    .producingArrangements="${this.producingArrangements}"
                    .schedulingWorkDateTimeLimit="${this.schedulingWorkTimeLimit}">
            </by-factory-timeline-components>
        `;
    }

    @property()
    schedulingWorkTimeLimit?: SchedulingWorkTimeLimit;

    @property()
    schedulingOrder: Array<SchedulingOrder> = new Array<SchedulingOrder>();

    @property()
    schedulingProduct: Array<SchedulingProduct> = new Array<SchedulingProduct>();

    @property()
    schedulingFactory: Array<SchedulingFactory> = new Array<SchedulingFactory>();

    @property()
    producingArrangements: Array<BaseProducingArrangement> = new Array<BaseProducingArrangement>();
}

declare global {
    interface HTMLElementTagNameMap {
        'scheduling-vis-timeline-panel': SchedulingVisTimelinePanel;
    }
}
