package car;

import car.monitor.*;
import car.control.ControlCenter;

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
    private final JToggleButton pauseBtn = new JToggleButton("暂停全局");
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
        JPanel south = new JPanel(new BorderLayout());
        south.add(playbackPanel, BorderLayout.CENTER);
        JPanel controlBar = new JPanel();
        controlBar.add(pauseBtn);
        south.add(controlBar, BorderLayout.EAST);
        f.add(south, BorderLayout.SOUTH);
        f.setDefaultCloseOperation(EXIT_ON_CLOSE);
        f.setVisible(true);
        MonitorCenter.addCriticalListener(frameIndex -> playbackPanel.markEvent(frameIndex));
        pauseBtn.addActionListener(e -> {
            if (pauseBtn.isSelected()){
                pauseBtn.setText("已暂停");
                ControlCenter.pause();
            }else{
                pauseBtn.setText("暂停全局");
                ControlCenter.resume();
            }
        });
        recorder.record(fieldMatrix, snapshotCars(),"init", MonitorCenter.getCurrentFrame());
        playbackPanel.updateFrames(recorder.getFrames());
        tickPanel.setPageByFrame(0);
    }

    public PlaybackRecorder getRecorder(){
        return recorder;
    }

    public void showSnapshot(PlaybackFrame frame, String overlay){
        this.replaySnapshot = frame.getSnapshot();
        this.overlay = overlay;
        this.liveMode = false;
        this.replayCars = frame.getCars();
        tickPanel.setPageByFrame(frame.getIndex());
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
            if (replayCars != null){
                for (CarSnapshot cs : replayCars){
                    Position p = cs.position;
                    g.setColor(cs.color);
                    g.fill3DRect(left + p.col * step, top + p.row * step, step, step, false);
                    String label = cs.name != null ? cs.name : "car-"+cs.id;
                    int stringWidth = fm.stringWidth(label);
                    g.setColor(Color.WHITE);
                    g.drawString(label, left + p.col * step + (step - stringWidth) / 2,
                            top + p.row * step + step / 2);
                }
            }
            g.setColor(Color.MAGENTA);
            g.drawString(overlay, left, top-5);
        }
    }

    @Override
    public void carCreated(Car car) {
        cars.add(car);
        recorder.record(fieldMatrix, snapshotCars(),"car-created-"+car.getIndex(), MonitorCenter.getCurrentFrame());
        playbackPanel.updateFrames(recorder.getFrames());
        tickPanel.setPageByFrame(MonitorCenter.getCurrentFrame());
    }

    @Override
    public void carDestroyed(Car car) {
        cars.remove(car);
        recorder.record(fieldMatrix, snapshotCars(),"car-destroyed-"+car.getIndex(), MonitorCenter.getCurrentFrame());
        playbackPanel.updateFrames(recorder.getFrames());
        tickPanel.setPageByFrame(MonitorCenter.getCurrentFrame());
    }

    @Override
    public void carMoved(Car car, Position from, Position to, boolean success) {
        if (!success){
            criticalHighlight = to;
            overlay = "冲突: "+to;
            playbackPanel.markEvent(recorder.size());
        }else{
            criticalHighlight = to;
            overlay = "移动到: "+to;
        }
        recorder.record(fieldMatrix, snapshotCars(),"car-"+car.getIndex()+" move "+success, MonitorCenter.getCurrentFrame());
        playbackPanel.updateFrames(recorder.getFrames());
        tickPanel.setPageByFrame(MonitorCenter.getCurrentFrame());
        repaint();
    }

    @Override
    public void fieldChanged() {
        recorder.record(fieldMatrix, snapshotCars(),"field-change", MonitorCenter.getCurrentFrame());
        playbackPanel.updateFrames(recorder.getFrames());
        tickPanel.setPageByFrame(MonitorCenter.getCurrentFrame());
        repaint();
    }

    public boolean isLiveMode(){
        return liveMode;
    }

    private java.util.List<CarSnapshot> snapshotCars(){
        java.util.List<CarSnapshot> result = new java.util.ArrayList<>();
        for (Car c : cars){
            result.add(new CarSnapshot(c.getIndex(), c.getName(), c.getColor(), c.getPosition()));
        }
        return result;
    }

    private java.util.List<CarSnapshot> replayCars;
}
