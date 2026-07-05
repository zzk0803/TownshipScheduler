package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;

@Tag("lit-timer")
@JsModule("./components/lit-timer.ts")
public class LitTimer
        extends Component
        implements HasSize, HasText, HasStyle {

    public LitTimer() {
        // 默认配置
    }

    // --- 属性设置 ---

    public void setMode(Mode mode) {
        getElement().setProperty("mode", mode.name().toLowerCase());
    }

    public void setDurationMs(int durationMs) {
        getElement().setProperty("durationMs", durationMs);
    }

    public void setShowMilliseconds(boolean show) {
        getElement().setProperty("showMilliseconds", show);
    }

    // --- 控制方法 ---

    public void start() {
        getElement().callJsFunction("start");
    }

    public void pause() {
        getElement().callJsFunction("pause");
    }

    public void reset() {
        getElement().callJsFunction("reset");
    }

    // --- 事件监听 ---

    public Registration addTickListener(ComponentEventListener<TickEvent> listener) {
        return addListener(TickEvent.class, listener);
    }

    public Registration addFinishListener(ComponentEventListener<FinishEvent> listener) {
        return addListener(FinishEvent.class, listener);
    }

    // --- 枚举与事件类 ---

    public enum Mode {
        COUNTUP, COUNTDOWN
    }

    @DomEvent("tick")
    public static class TickEvent
            extends ComponentEvent<LitTimer> {

        private final long timeMs;

        public TickEvent(
                LitTimer source, boolean fromClient,
                @EventData("event.detail.timeMs") long timeMs
        ) {
            super(source, fromClient);
            this.timeMs = timeMs;
        }

        public long getTimeMs() {
            return timeMs;
        }

    }

    @DomEvent("finish")
    public static class FinishEvent
            extends ComponentEvent<LitTimer> {

        public FinishEvent(LitTimer source, boolean fromClient) {
            super(source, fromClient);
        }

    }

}
