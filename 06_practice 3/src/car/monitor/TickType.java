package car.monitor;

/**
 * 类型区分：原子调用、行为调用（组合调用）、关键事件以及线程状态快照。
 */
public enum TickType {
    ATOMIC,
    BEHAVIOR_START,
    BEHAVIOR_END,
    CRITICAL,
    THREAD
}
