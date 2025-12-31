package car.control;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 全局控制面板：用于暂停/恢复所有小车的动作生成。
 */
public final class ControlCenter {
    private static final AtomicBoolean paused = new AtomicBoolean(false);

    private ControlCenter(){}

    public static void pause(){
        paused.set(true);
    }

    public static void resume(){
        paused.set(false);
    }

    public static boolean isPaused(){
        return paused.get();
    }
}
