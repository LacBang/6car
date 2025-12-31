package car.monitor;

/**
 * tick 事件用于实时监视与回放。
 */
public class TickEvent {
    private final long timestamp;
    private final TickType type;
    private final String message;
    private final String threadName;
    private final int sequence;

    public TickEvent(long timestamp, TickType type, String message, String threadName, int sequence) {
        this.timestamp = timestamp;
        this.type = type;
        this.message = message;
        this.threadName = threadName;
        this.sequence = sequence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public TickType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getThreadName() {
        return threadName;
    }

    public int getSequence() {
        return sequence;
    }

    @Override
    public String toString() {
        return "["+sequence+"] "+type+" @"+timestamp+" ("+threadName+"): "+message;
    }
}
