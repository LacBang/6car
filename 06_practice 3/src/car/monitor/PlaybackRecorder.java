package car.monitor;

import car.MatrixField;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 记录地图快照，并为回放提供列表。
 */
public class PlaybackRecorder {
    private final List<PlaybackFrame> frames = Collections.synchronizedList(new ArrayList<>());
    private final File logFile;

    public PlaybackRecorder(){
        File dir = new File("out/replay");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        logFile = new File(dir, "replay-"+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+".log");
    }

    public void record(MatrixField field, java.util.List<CarSnapshot> cars, String reason, int frameIndex, ActionOverlay overlay){
        MatrixField.CellState[][] snapshot = copy(field);
        PlaybackFrame frame = new PlaybackFrame(snapshot, System.nanoTime(), reason, cars, frameIndex, overlay);
        frames.add(frame);
        appendToFile(frame);
    }

    public List<PlaybackFrame> getFrames(){
        return frames;
    }

    public int size(){
        return frames.size();
    }

    private MatrixField.CellState[][] copy(MatrixField field){
        MatrixField.CellState[][] snapshot = new MatrixField.CellState[field.getRows()][field.getCols()];
        for (int r=0;r<field.getRows();r++){
            for (int c=0;c<field.getCols();c++){
                snapshot[r][c] = field.getCellState(r,c);
            }
        }
        return snapshot;
    }

    private void appendToFile(PlaybackFrame frame){
        try(FileWriter writer = new FileWriter(logFile,true)){
            writer.write(frame.getTimestamp()+","+frame.getReason());
            writer.write(System.lineSeparator());
        }catch(IOException ignored){}
    }
}
