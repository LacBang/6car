package car;

import car.monitor.MonitorCenter;
import car.monitor.PlaybackPanel;
import car.monitor.PlaybackRecorder;
import car.monitor.TickPanel;

import javax.swing.*;
import java.awt.*;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class CarPainter extends JPanel implements CarEventsListener {

    private final MatrixField fieldMatrix;
    private final static int defaultCellSize = 50;
    private final static int minGap = 20;

    private final java.util.concurrent.CopyOnWriteArrayList<Car> cars = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final PlaybackRecorder recorder;
    private final PlaybackPanel playbackPanel;
    private final TickPanel tickPanel;
    private boolean liveMode = true;
    private MatrixField.CellState[][] replaySnapshot;
    private String overlay = "";
    private Position criticalHighlight;

    public CarPainter(MatrixField fieldMatrix) {
        super();
        this.fieldMatrix = fieldMatrix;
        this.recorder = new PlaybackRecorder();
        this.tickPanel = new TickPanel();
        MonitorCenter.addListener(tickPanel);
        JFrame f = new JFrame("Cars");
        setBackground(Color.LIGHT_GRAY);
        f.setLayout(new BorderLayout());
        f.setSize(fieldMatrix.getCols() * defaultCellSize + 260,
                fieldMatrix.getRows() * defaultCellSize + 120);
        f.add(this, BorderLayout.CENTER);
        f.add(tickPanel, BorderLayout.EAST);
        playbackPanel = new PlaybackPanel(this);
        f.add(playbackPanel, BorderLayout.SOUTH);
        f.setDefaultCloseOperation(EXIT_ON_CLOSE);
        f.setVisible(true);
        recorder.record(fieldMatrix,"init");
        playbackPanel.updateFrames(recorder.getFrames());
    }

    public PlaybackRecorder getRecorder(){
        return recorder;
    }

    public void showSnapshot(MatrixField.CellState[][] snapshot, String overlay){
        this.replaySnapshot = snapshot;
        this.overlay = overlay;
        this.liveMode = false;
        repaint();
    }

    public void useLiveMode(){
        this.liveMode = true;
        this.overlay = "";
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.BLACK);
        int gridWidth = screenWidth - 2 * minGap;
        int gridHeight = screenHeight - 2 * minGap;
        int step = Math.min(gridWidth / fieldMatrix.getCols(), gridHeight / fieldMatrix.getRows());

        int verticalGap = (gridHeight - step * fieldMatrix.getRows()) / 2 + minGap;
        int horizontalGap = (gridWidth - step * fieldMatrix.getCols()) / 2 + minGap;
        int left = horizontalGap;
        int top = verticalGap;
        int right = left + fieldMatrix.getCols() * step;
        int bottom = top + fieldMatrix.getRows() * step;

        // Drawing vertical lines
        for (int i = 0; i <= fieldMatrix.getCols(); i++) {
            g.drawLine(left + i * step, top, left + i * step, bottom);
        }
        // Drawing horizontal lines
        for (int i = 0; i <= fieldMatrix.getRows(); i++) {
            g.drawLine(left, top + i * step, right, top + i * step);
        }

        MatrixField.CellState[][] snapshot = replaySnapshot;
        if (liveMode || snapshot == null){
            // 实时模式：直接读取矩阵与小车列表
            for (int i = 0; i < fieldMatrix.getRows(); i++)
                for (int j = 0; j < fieldMatrix.getCols(); j++) {
                    if (fieldMatrix.getCellState(i, j) == MatrixField.CellState.WALL) {
                        g.setColor(Color.RED);
                        g.fill3DRect(left + j * step, top + i * step, step, step, false);
                        g.setColor(Color.BLACK);
                    }
                }
            for (Car car : cars) {
                Position p = car.getPosition();
                g.setColor(car.getColor());
                g.fill3DRect(left + p.col * step, top + p.row * step, step, step, false);
                if (car.getName() != null) {
                    int stringWidth = fm.stringWidth(car.getName());
                    g.setColor(Color.WHITE);
                    g.drawString(car.getName(), left + p.col * step + (step - stringWidth) / 2,
                            top + p.row * step + step / 2);
                }
            }
            if (criticalHighlight != null){
                g.setColor(Color.RED);
                g.drawRect(left + criticalHighlight.col * step, top + criticalHighlight.row * step, step, step);
            }
        }else{
            // 回放模式：按照快照绘制
            for (int i = 0; i < fieldMatrix.getRows(); i++)
                for (int j = 0; j < fieldMatrix.getCols(); j++) {
                    if (snapshot[i][j] == MatrixField.CellState.WALL) {
                        g.setColor(Color.RED);
                        g.fill3DRect(left + j * step, top + i * step, step, step, false);
                        g.setColor(Color.BLACK);
                    }else if (snapshot[i][j] == MatrixField.CellState.CAR){
                        g.setColor(Color.BLUE);
                        g.fill3DRect(left + j * step, top + i * step, step, step, false);
                    }
            }
            g.setColor(Color.MAGENTA);
            g.drawString(overlay, left, top-5);
        }
    }

    @Override
    public void carCreated(Car car) {
        cars.add(car);
        recorder.record(fieldMatrix,"car-created-"+car.getIndex());
        playbackPanel.updateFrames(recorder.getFrames());
    }

    @Override
    public void carDestroyed(Car car) {
        cars.remove(car);
        recorder.record(fieldMatrix,"car-destroyed-"+car.getIndex());
        playbackPanel.updateFrames(recorder.getFrames());
    }

    @Override
    public void carMoved(Car car, Position from, Position to, boolean success) {
        if (!success){
            criticalHighlight = to;
            overlay = "冲突: "+to;
        }else{
            criticalHighlight = null;
        }
        recorder.record(fieldMatrix,"car-"+car.getIndex()+" move "+success);
        playbackPanel.updateFrames(recorder.getFrames());
        repaint();
    }

    @Override
    public void fieldChanged() {
        recorder.record(fieldMatrix,"field-change");
        playbackPanel.updateFrames(recorder.getFrames());
        repaint();
    }

    public boolean isLiveMode(){
        return liveMode;
    }
}
