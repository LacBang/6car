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
    private final JToggleButton playToggle = new JToggleButton("暂停");
    private final JButton prev = new JButton("上一帧");
    private final JButton next = new JButton("下一帧");
    private final JCheckBox followRealtime = new JCheckBox("跟随实时", true);
    private final JSlider slider = new JSlider();
    private final AtomicBoolean playingReplay = new AtomicBoolean(false);
    private List<PlaybackFrame> frames;
    private int index = 0;
    private final CarPainter painter;
    private final java.util.Set<Integer> marks = new java.util.HashSet<>();

    public PlaybackPanel(CarPainter painter){
        super(new FlowLayout(FlowLayout.LEFT));
        this.painter = painter;
        playToggle.setSelected(true);
        playToggle.setText("暂停");
        add(playToggle);
        add(followRealtime);
        add(prev);
        add(next);
        add(new JLabel("帧："));
        slider.setMinimum(0);
        slider.setMaximum(0);
        slider.setPreferredSize(new Dimension(200,30));
        add(slider);

        playToggle.addActionListener(e -> {
            if (followRealtime.isSelected()){
                if (playToggle.isSelected()){
                    playToggle.setText("暂停");
                    car.control.ControlCenter.resume();
                }else{
                    playToggle.setText("播放");
                    car.control.ControlCenter.pause();
                }
            }else{
                if (playToggle.isSelected()){
                    playToggle.setText("暂停");
                    playingReplay.set(true);
                }else{
                    playToggle.setText("播放");
                    playingReplay.set(false);
                }
            }
        });
        prev.addActionListener(e -> step(-1));
        next.addActionListener(e -> step(1));
        followRealtime.addActionListener(e -> {
            if (followRealtime.isSelected()){
                playingReplay.set(false);
                slider.setValue(slider.getMaximum());
                applyFrame(slider.getMaximum());
                car.control.ControlCenter.resume();
            }else{
                car.control.ControlCenter.pause();
            }
        });
        slider.addChangeListener(e -> {
            if (frames == null || followRealtime.isSelected()) return;
            index = slider.getValue();
            applyFrame(index);
        });

        Timer timer = new Timer(200, e -> {
            if (frames == null || followRealtime.isSelected()) return;
            if (playingReplay.get()){
                step(1);
            }
        });
        timer.start();
    }

    public void updateFrames(List<PlaybackFrame> frames){
        this.frames = frames;
        slider.setMaximum(Math.max(0, frames.size()-1));
        refreshLabels();
        if (followRealtime.isSelected() && frames != null && !frames.isEmpty()){
            index = frames.size()-1;
            slider.setValue(index);
            applyFrame(index);
        }
    }

    public void step(int delta){
        if (frames == null || frames.isEmpty()) return;
        followRealtime.setSelected(false);
        index = Math.max(0, Math.min(frames.size()-1, index+delta));
        slider.setValue(index);
        applyFrame(index);
    }

    private void applyFrame(int idx){
        if (frames == null || idx >= frames.size()) return;
        PlaybackFrame frame = frames.get(idx);
        painter.showSnapshot(frame, "回放："+frame.getReason());
    }

    public boolean isRealtime(){
        return followRealtime.isSelected();
    }

    public boolean isFollowRealtime(){
        return followRealtime.isSelected();
    }

    public void switchToReplay(){
        followRealtime.setSelected(false);
    }

    public void markEvent(int frameIndex){
        marks.add(frameIndex);
        refreshLabels();
    }

    private void refreshLabels(){
        java.util.Hashtable<Integer, JLabel> table = new java.util.Hashtable<>();
        for (Integer m : marks){
            table.put(m, new JLabel("*"));
        }
        if (table.isEmpty()){
            slider.setPaintLabels(false);
            slider.setLabelTable(null);
        }else{
            slider.setLabelTable(table);
            slider.setPaintLabels(true);
        }
    }
}
