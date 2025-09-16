import {css, html, LitElement, PropertyValues} from 'lit';
import {customElement, property, query} from 'lit/decorators.js';
import {vaadinStyles, visStyles, visTownshipStyles} from "./external-styles";
import {DataGroup, DataItem, Timeline, TimelineOptions} from "vis-timeline";

@customElement('lit-vis-timeline')
export class LitVisTimeline
    extends LitElement {

    static styles = [
        visStyles,
        vaadinStyles,
        visTownshipStyles,
        css`
            :host {
                display: flex;
                flex-direction: column;
            }

            #vis-container {
                flex: 1 1 auto;
            }
        `
    ]

    @property()
    fromDateTime?: string;

    @property()
    toDateTime?: string;

    @property()
    datasets: DataItem[] = [];

    @property()
    groups: DataGroup[] = [];

    @property()
    options!: TimelineOptions;

    @query("#vis-container")
    visContainerElement!: HTMLDivElement;

    timeline!: Timeline;

    connectedCallback() {
        super.connectedCallback();
        this.classList.add("flex", "flex-col", "w-full", "h-auto");
    }

    render() {
        return html`
            <div id="vis-container">
            </div>
        `;
    }

    firstUpdated(_changedProperties: PropertyValues) {
        // Create a new Timeline in the vis-container
        this.setupTimeline();
    }

    protected willUpdate(_changedProperties: PropertyValues) {
        super.willUpdate(_changedProperties)

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

    private setupTimeline() {
        this.timeline = new Timeline(
            this.visContainerElement,
            this.datasets,
            this.groups,
            this.options
        );
        this.timeline.setWindow(this.fromDateTime, this.toDateTime);
    }
}
