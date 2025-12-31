package car;

import car.control.ControlCenter;
import car.monitor.ActionOverlay;
import car.monitor.ActionPreviewPanel;
import car.monitor.CarSnapshot;
import car.monitor.MonitorCenter;
import car.monitor.PlaybackFrame;
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
    private final ActionPreviewPanel previewPanel;
    private final JCheckBox followTick = new JCheckBox("跟随最新", true);
    private final JToggleButton pauseBtn = new JToggleButton("暂停全局");
    private boolean liveMode = true;
    private MatrixField.CellState[][] replaySnapshot;
    private String overlay = "";
    private Position criticalHighlight;
    private ActionOverlay lastOverlay;

    public CarPainter(MatrixField fieldMatrix) {
        super();
        this.fieldMatrix = fieldMatrix;
        this.recorder = new PlaybackRecorder();
        this.tickPanel = new TickPanel();
        this.previewPanel = new ActionPreviewPanel();
        MonitorCenter.addListener(tickPanel);
        JFrame f = new JFrame("Cars");
        setBackground(Color.LIGHT_GRAY);
        f.setLayout(new BorderLayout());
        f.setSize(fieldMatrix.getCols() * defaultCellSize + 260,
                fieldMatrix.getRows() * defaultCellSize + 120);
        f.add(this, BorderLayout.CENTER);
        JPanel east = new JPanel(new BorderLayout());
        east.setPreferredSize(new Dimension(300, fieldMatrix.getRows() * defaultCellSize));
        east.add(tickPanel, BorderLayout.CENTER);
        east.add(previewPanel, BorderLayout.SOUTH);
        f.add(east, BorderLayout.EAST);
        playbackPanel = new PlaybackPanel(this);
        JPanel south = new JPanel(new BorderLayout());
        south.add(playbackPanel, BorderLayout.CENTER);
        JPanel controlBar = new JPanel();
        controlBar.add(pauseBtn);
        controlBar.add(followTick);
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
        recorder.record(fieldMatrix, snapshotCars(),"init", MonitorCenter.getCurrentFrame(), null);
        playbackPanel.updateFrames(recorder.getFrames());
        previewPanel.update(recorder.getFrames());
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
        this.lastOverlay = frame.getOverlay();
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
            paintOverlayArrow(g, left, top, step, lastOverlay);
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
            paintOverlayArrow(g, left, top, step, lastOverlay);
            g.setColor(Color.MAGENTA);
            g.drawString(overlay, left, top-5);
        }
    }

    @Override
    public void carCreated(Car car) {
        cars.add(car);
        recorder.record(fieldMatrix, snapshotCars(),"car-created-"+car.getIndex(), MonitorCenter.getCurrentFrame(), null);
        playbackPanel.updateFrames(recorder.getFrames());
        previewPanel.update(recorder.getFrames());
        if (followTick.isSelected()) tickPanel.setPageByFrame(MonitorCenter.getCurrentFrame());
    }

    @Override
    public void carDestroyed(Car car) {
        cars.remove(car);
        recorder.record(fieldMatrix, snapshotCars(),"car-destroyed-"+car.getIndex(), MonitorCenter.getCurrentFrame(), null);
        playbackPanel.updateFrames(recorder.getFrames());
        previewPanel.update(recorder.getFrames());
        if (followTick.isSelected()) tickPanel.setPageByFrame(MonitorCenter.getCurrentFrame());
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
        lastOverlay = new ActionOverlay(from, to, overlay);
        recorder.record(fieldMatrix, snapshotCars(),"car-"+car.getIndex()+" move "+success, MonitorCenter.getCurrentFrame(), lastOverlay);
        playbackPanel.updateFrames(recorder.getFrames());
        previewPanel.update(recorder.getFrames());
        if (followTick.isSelected()) tickPanel.setPageByFrame(MonitorCenter.getCurrentFrame());
        repaint();
    }

    @Override
    public void fieldChanged() {
        recorder.record(fieldMatrix, snapshotCars(),"field-change", MonitorCenter.getCurrentFrame(), null);
        playbackPanel.updateFrames(recorder.getFrames());
        previewPanel.update(recorder.getFrames());
        if (followTick.isSelected()) tickPanel.setPageByFrame(MonitorCenter.getCurrentFrame());
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

    private void paintOverlayArrow(Graphics g, int left, int top, int step, ActionOverlay overlay){
        if (overlay == null || overlay.from == null || overlay.to == null) return;
        g.setColor(Color.ORANGE);
        int x1 = left + overlay.from.col * step + step/2;
        int y1 = top + overlay.from.row * step + step/2;
        int x2 = left + overlay.to.col * step + step/2;
        int y2 = top + overlay.to.row * step + step/2;
        g.drawLine(x1, y1, x2, y2);
        g.fillOval(x2-4, y2-4, 8,8);
        g.drawString("前→后", Math.min(x1,x2), Math.min(y1,y2)-2);
    }
}
