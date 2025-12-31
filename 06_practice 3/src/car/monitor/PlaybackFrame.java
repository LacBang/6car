package car.monitor;

import car.MatrixField;

/**
 * 用于回放的帧快照。
 */
public class PlaybackFrame {
    private final MatrixField.CellState[][] snapshot;
    private final long timestamp;
    private final String reason;
    private final java.util.List<CarSnapshot> cars;
    private final int index;

    public PlaybackFrame(MatrixField.CellState[][] snapshot, long timestamp, String reason, java.util.List<CarSnapshot> cars, int index) {
        this.snapshot = snapshot;
        this.timestamp = timestamp;
        this.reason = reason;
        this.cars = cars;
        this.index = index;
    }

    public MatrixField.CellState[][] getSnapshot() {
        return snapshot;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getReason() {
        return reason;
    }

    public java.util.List<CarSnapshot> getCars() {
        return cars;
    }

    public int getIndex() {
        return index;
    }
}
