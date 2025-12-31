package car.monitor;

import car.MatrixField;
import car.CarPainter;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 回放控制区：可以切换实时/回放模式。
 */
public class PlaybackPanel extends JPanel {
    private final JButton play = new JButton("播放");
    private final JButton pause = new JButton("暂停");
    private final JButton prev = new JButton("上一帧");
    private final JButton next = new JButton("下一帧");
    private final JCheckBox realtime = new JCheckBox("实时", true);
    private final JSlider slider = new JSlider();
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private List<PlaybackFrame> frames;
    private int index = 0;
    private final CarPainter painter;

    public PlaybackPanel(CarPainter painter){
        super(new FlowLayout(FlowLayout.LEFT));
        this.painter = painter;
        add(realtime);
        add(play);
        add(pause);
        add(prev);
        add(next);
        add(new JLabel("帧："));
        slider.setMinimum(0);
        slider.setMaximum(0);
        slider.setPreferredSize(new Dimension(200,30));
        add(slider);

        play.addActionListener(e -> playing.set(true));
        pause.addActionListener(e -> playing.set(false));
        prev.addActionListener(e -> step(-1));
        next.addActionListener(e -> step(1));
        realtime.addActionListener(e -> {
            if (realtime.isSelected()){
                playing.set(false);
                painter.useLiveMode();
            }
        });
        slider.addChangeListener(e -> {
            if (frames == null || realtime.isSelected()) return;
            index = slider.getValue();
            applyFrame(index);
        });

        Timer timer = new Timer(200, e -> {
            if (frames == null || realtime.isSelected()) return;
            if (playing.get()){
                step(1);
            }
        });
        timer.start();
    }

    public void updateFrames(List<PlaybackFrame> frames){
        this.frames = frames;
        slider.setMaximum(Math.max(0, frames.size()-1));
    }

    public void step(int delta){
        if (frames == null || frames.isEmpty()) return;
        realtime.setSelected(false);
        index = Math.max(0, Math.min(frames.size()-1, index+delta));
        slider.setValue(index);
        applyFrame(index);
    }

    private void applyFrame(int idx){
        if (frames == null || idx >= frames.size()) return;
        PlaybackFrame frame = frames.get(idx);
        painter.showSnapshot(frame.getSnapshot(), "回放："+frame.getReason());
    }

    public boolean isRealtime(){
        return realtime.isSelected();
    }

    public void switchToReplay(){
        realtime.setSelected(false);
    }
}
