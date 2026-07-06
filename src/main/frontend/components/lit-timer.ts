import {css, html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';

@customElement('lit-timer')
export class LitTimer
    extends LitElement {
    static styles = css`
        :host {
            display: inline-block;
            font-family: var(--flow-timer-font-family, var(--lumo-font-family));
            font-size: var(--flow-timer-font-size, var(--lumo-font-size-xl));
            color: var(--flow-timer-color, var(--lumo-body-text-color));
            font-weight: var(--flow-timer-font-weight, 500);
            letter-spacing: 0.05em;
            line-height: 1.2;
            font-variant-numeric: tabular-nums;
        }

        :host([theme~="small"]) {
            --flow-timer-font-size: var(--lumo-font-size-s);
        }

        :host([theme~="large"]) {
            --flow-timer-font-size: var(--lumo-font-size-xxxl);
        }

        :host([theme~="primary"]) {
            --flow-timer-color: var(--lumo-primary-color);
        }

        :host([theme~="success"]) {
            --flow-timer-color: var(--lumo-success-color);
        }

        :host([theme~="error"]) {
            --flow-timer-color: var(--lumo-error-color);
        }

        :host([theme~="warning"]) {
            --flow-timer-color: var(--lumo-warning-color);
        }

        :host([theme~="secondary"]) {
            --flow-timer-color: var(--lumo-secondary-text-color);
        }

        .ms {
            font-size: 0.75em;
            opacity: 0.7;
            margin-left: 0.1em;
        }
    `;

    // --- 暴露给 Vaadin 的属性 ---
    @property({type: String})
    mode: 'countup' | 'countdown' = 'countup';

    @property({type: Number})
    durationMs: number = 0; // 倒计时总时长(毫秒)

    @property({type: Boolean})
    running: boolean = false;

    @property({type: Boolean})
    showMilliseconds: boolean = false;

    // --- 内部状态 ---
    @state()
    private currentTimeMs: number = 0;

    private startTime: number = 0;

    private accumulatedTime: number = 0;

    private rafId: number | null = null;

    constructor() {
        super();
        this.currentTimeMs = this.mode === 'countdown'
            ? this.durationMs
            : 0;
    }

    // --- 暴露给 Vaadin 调用的方法 ---
    public start() {
        this.running = true;
    }

    public pause() {
        this.running = false;
    }

    public reset() {
        this.running = false;
        this.accumulatedTime = 0;
        this.currentTimeMs = this.mode === 'countdown'
            ? this.durationMs
            : 0;
    }

    // 处理组件被移除出 DOM 的情况，防止内存泄漏和时间错乱
    disconnectedCallback() {
        super.disconnectedCallback();
        if (this.rafId !== null) {
            cancelAnimationFrame(this.rafId);
            this.rafId = null;
            if (this.running) {
                this.accumulatedTime += performance.now() - this.startTime;
            }
        }
    }

    connectedCallback() {
        super.connectedCallback();
        if (this.running && this.rafId === null) {
            this.startTime = performance.now();
            this.rafId = requestAnimationFrame(() => this.tick());
        }
    }

    render() {
        const {main, ms} = this.formatTime(this.currentTimeMs);
        if (this.showMilliseconds) {
            return html`<span>${main}<span class="ms">.${ms}</span></span>`;
        }
        return html`<span>${main}</span>`;
    }

    protected updated(changedProperties: PropertyValues) {
        super.updated(changedProperties);

        // 监听 running 属性变化，控制计时器启停
        if (changedProperties.has('running')) {
            if (this.running) {
                this.startTime = performance.now();
                this.rafId = requestAnimationFrame(() => this.tick());
            } else {
                this.stopAnimation();
            }
        }

        // 监听模式或时长变化，重置时间
        if (changedProperties.has('mode') || changedProperties.has('durationMs')) {
            if (!this.running) {
                this.currentTimeMs = this.mode === 'countdown'
                    ? this.durationMs
                    : 0;
            }
        }
    }

    // 核心计时循环
    private tick() {
        if (!this.running) return;

        const now = performance.now();
        const elapsed = this.accumulatedTime + (now - this.startTime);

        if (this.mode === 'countdown') {
            const remaining = this.durationMs - elapsed;
            if (remaining <= 0) {
                this.currentTimeMs = 0;
                this.running = false; // 触发 property change，自动停止
                this.dispatchEvent(new CustomEvent('finish'));
                return;
            }
            this.currentTimeMs = remaining;
        } else {
            this.currentTimeMs = elapsed;
        }

        // 派发 tick 事件，携带当前时间
        this.dispatchEvent(new CustomEvent('tick', {
            detail: {timeMs: this.currentTimeMs}
        }));

        this.rafId = requestAnimationFrame(() => this.tick());
    }

    private stopAnimation() {
        if (this.rafId !== null) {
            cancelAnimationFrame(this.rafId);
            this.rafId = null;
        }
        // 记录已消耗时间，防止暂停后时间丢失
        this.accumulatedTime += performance.now() - this.startTime;
    }

    // 优化了格式化方法，将毫秒部分拆分以便应用不同样式
    private formatTime(ms: number): { main: string, ms: string } {
        const totalSeconds = Math.floor(ms / 1000);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        const milliseconds = Math.floor((ms % 1000) / 10);

        const pad = (n: number) => n.toString()
            .padStart(2, '0');
        let mainStr = hours > 0
            ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
            : `${pad(minutes)}:${pad(seconds)}`;
        let msStr = pad(milliseconds);

        return {main: mainStr, ms: msStr};
    }
}
