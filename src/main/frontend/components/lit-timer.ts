import {css, html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';

@customElement('lit-timer')
export class LitTimer
    extends LitElement {
    static styles = css`
        :host {
            display: inline-block;
            font-family: monospace;
            font-size: 1.5rem;
            font-variant-numeric: tabular-nums; /* 保证数字等宽，防止跳动 */
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
        return html`<span>${this.formatTime(this.currentTimeMs)}</span>`;
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

    // 格式化时间显示
    private formatTime(ms: number): string {
        const totalSeconds = Math.floor(ms / 1000);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        const milliseconds = Math.floor((ms % 1000) / 10);

        const pad = (n: number) => n.toString()
            .padStart(2, '0');
        let timeStr = hours > 0
            ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
            : `${pad(minutes)}:${pad(seconds)}`;

        if (this.showMilliseconds) {
            timeStr += `.${pad(milliseconds)}`;
        }
        return timeStr;
    }
}
