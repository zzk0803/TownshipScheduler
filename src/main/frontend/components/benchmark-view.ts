import {css, html, LitElement} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/vertical-layout';
import { unsafeHTML } from "lit/directives/unsafe-html.js";

@customElement('benchmark-view')
export class BenchmarkView
    extends LitElement {

    @property()
    content: string = ``;

    render() {
        return html`
            ${unsafeHTML(this.content)}
        `;
    }


    protected createRenderRoot(): HTMLElement | DocumentFragment {
        return this;
    }
}

declare global {
    interface HTMLElementTagNameMap {
        'benchmark-view': BenchmarkView;
    }
}
