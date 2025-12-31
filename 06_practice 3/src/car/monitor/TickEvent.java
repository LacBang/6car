package car.monitor;

/**
 * tick 事件用于实时监视与回放。
 */
public class TickEvent {
    private final TickType type;
    private final String message;
    private final int sequence;
    private final int frameIndex;
    private final int carId;
    private final int row;
    private final int col;

    public TickEvent(TickType type, String message, int sequence, int frameIndex, int carId, int row, int col) {
        this.type = type;
        this.message = message;
        this.sequence = sequence;
        this.frameIndex = frameIndex;
        this.carId = carId;
        this.row = row;
        this.col = col;
    }

    public TickType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public int getSequence() {
        return sequence;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public int getCarId() {
        return carId;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public String toString() {
        return "#" + carId + " " + message + " (frame " + frameIndex + ")";
    }
}
