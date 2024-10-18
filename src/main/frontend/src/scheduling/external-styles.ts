import { css, unsafeCSS } from 'lit';
import vistimelinegraph2dcss from 'vis-timeline/styles/vis-timeline-graph2d.css?inline';
import bootstrapcss from 'bootstrap/dist/css/bootstrap.css?inline';

export const visStyles = css`
    ${unsafeCSS(vistimelinegraph2dcss)},
` as any;

export const bootstrapStyles = css`
    ${unsafeCSS(bootstrapcss)}, 
` as any;
