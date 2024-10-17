import {css, html, LitElement, PropertyValues} from 'lit';
import {customElement, property, query} from 'lit/decorators.js';
import {DataGroup, DataItem, Timeline, TimelineOptions} from "vis-timeline";
// import {DataGroup, DataItem, Timeline, TimelineOptions} from "vis-timeline/standalone";
import {visStyles} from "./timeline-styles";

@customElement('lit-vis-timeline')
export class LitVisTimeline
    extends LitElement {

    @property({attribute: "fromDate"})
    dateWindowStartString?: string;

    @property({attribute: "toDate"})
    dataWindowEndString?: string;

    @property()
    datasets!: DataItem[];

    @property()
    groups!: DataGroup[];

    @property()
    options!: TimelineOptions;

    @query("#vis-container")
    visContainerElement!: HTMLDivElement;

    timeline!: Timeline;

    static styles = [
        visStyles,
        css`
            :host {
                display: flex;
                flex-direction: column;
            }

            #vis-container {
                flex: 1 1 auto;
            }

            .vis-time-axis .vis-grid.vis-saturday,
            .vis-time-axis .vis-grid.vis-sunday {
                background: #D3D7CFFF;
            }
        `
    ]


    render() {
        return html`
            <div id="vis-container">
            </div>
        `;
    }

    firstUpdated(_changedProperties: PropertyValues) {
        // Create a new Timeline in the vis-container
        this.timeline = new Timeline(
            this.visContainerElement,
            this.datasets,
            this.groups
        );
        this.timeline.setOptions(this.options);
        this.timeline.setWindow(this.dateWindowStartString, this.dataWindowEndString);
    }


    protected willUpdate(_changedProperties: PropertyValues) {
        console.log(this.datasets)
        if (this.timeline) {
            if (_changedProperties.has("datasets")) {
                this.timeline.setItems(this.datasets);
            }

            if (_changedProperties.has("groups")) {
                this.timeline.setGroups(this.groups);
            }

            if (_changedProperties.has("options")) {
                this.timeline.setOptions(this.options);
            }
        }
    }

    protected updated(_changedProperties: PropertyValues) {
        if (this.timeline) {
            this.timeline.redraw();
        }
        super.updated(_changedProperties);
    }
}
