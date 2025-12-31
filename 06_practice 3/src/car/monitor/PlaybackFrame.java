package car.monitor;

import car.MatrixField;

/**
 * 用于回放的帧快照。
 */
public class PlaybackFrame {
    private final MatrixField.CellState[][] snapshot;
    private final long timestamp;
    private final String reason;

    public PlaybackFrame(MatrixField.CellState[][] snapshot, long timestamp, String reason) {
        this.snapshot = snapshot;
        this.timestamp = timestamp;
        this.reason = reason;
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
}
