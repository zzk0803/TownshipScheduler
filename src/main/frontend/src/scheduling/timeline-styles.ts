import { css, unsafeCSS } from 'lit';
import vistimelinegraph2dcss from 'vis-timeline/styles/vis-timeline-graph2d.css?inline';

export const visStyles = css`
    ${unsafeCSS(vistimelinegraph2dcss)}, 
` as any;
