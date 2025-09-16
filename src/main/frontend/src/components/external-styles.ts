import {css, unsafeCSS} from 'lit';
// @ts-ignore
import vistimelinegraph2dcss from 'vis-timeline/styles/vis-timeline-graph2d.css?inline';
import {utility} from "@vaadin/vaadin-lumo-styles/utility";

export const visStyles = css`
    ${unsafeCSS(vistimelinegraph2dcss)},
` as any;

export const vaadinStyles = css`
    ${unsafeCSS(utility)},
` as any;

export const visTownshipStyles=css`
    .vis-item.arrange {
        background-color: transparent;
        border-style: dashed !important;
        z-index: 0;
    }
`as any
