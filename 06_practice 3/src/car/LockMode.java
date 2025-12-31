package car;

/**
 * 锁策略枚举，便于在 Main.MOE 中快速切换不同并发控制实现。
 */
public enum LockMode {
    GLOBAL_SINGLE,
    THREE_STATE,
    ORDERED_PAIR,
    TARGET_SINGLE
}
