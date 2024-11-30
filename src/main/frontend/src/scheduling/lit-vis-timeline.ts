import {css, html, LitElement, PropertyValues, unsafeCSS} from 'lit';
import {customElement, property, query} from 'lit/decorators.js';
import {DataGroup, DataItem, Timeline, TimelineOptions} from "vis-timeline";
import {utility} from "@vaadin/vaadin-lumo-styles/utility";
import vistimelinegraph2dcss from 'vis-timeline/styles/vis-timeline-graph2d.css?inline';

export const visStyles = css`
    ${unsafeCSS(vistimelinegraph2dcss)},
` as any;


@customElement('lit-vis-timeline')
export class LitVisTimeline
    extends LitElement {

    static styles = [
        visStyles,
        utility,
        css`
        `
    ]

    protected willUpdate(_changedProperties: PropertyValues) {
        console.log(this.datasets)
        if (this.timeline) {
            if (_changedProperties.has("datasets")) {
                this.timeline.setItems(this.datasets);
            }

            if (_changedProperties.has("groups")) {
                this.timeline.setGroups(this.groups);
            }
        }
    }

    protected updated(_changedProperties: PropertyValues) {
        if (this.timeline) {
            this.timeline.redraw();
        }
        super.updated(_changedProperties);
    }

    firstUpdated(_changedProperties: PropertyValues) {
        // Create a new Timeline in the vis-container
        this.timeline = new Timeline(
            this.visContainerElement,
            this.datasets,
            this.groups
        );
        this.timeline.setOptions(this.options);
        // this.timeline.setWindow(this.dateWindowStartString, this.dataWindowEndString);
    }

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

    render() {
        return html`
            <div id="vis-container" class="flex flex-col flex-auto max-w-full">
            </div>
        `;
    }
}
