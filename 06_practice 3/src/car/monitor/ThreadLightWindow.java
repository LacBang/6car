package car.monitor;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每个线程一盏灯：运行=绿，等待锁=红。
 */
public class ThreadLightWindow extends JFrame implements ThreadStateListener, WaitingLightListener {
    private final Map<Integer, JLabel> lights = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> waiting = new ConcurrentHashMap<>();

    public ThreadLightWindow(){
        super("线程灯控");
        setLayout(new GridLayout(0,1));
        setSize(200,400);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void onThreadState(int carId, Thread.State state) {
        updateLight(carId, waiting.getOrDefault(carId,false));
    }

    @Override
    public void onWaiting(int carId, boolean isWaiting) {
        setWaiting(carId, isWaiting);
    }

    public void setWaiting(int carId, boolean isWaiting){
        waiting.put(carId, isWaiting);
        updateLight(carId, isWaiting);
    }

    private void updateLight(int carId, boolean isWaiting){
        JLabel label = lights.computeIfAbsent(carId, id -> {
            JLabel l = new JLabel("car-"+id, SwingConstants.CENTER);
            l.setOpaque(true);
            add(l);
            return l;
        });
        label.setBackground(isWaiting ? Color.RED : Color.GREEN);
        label.setForeground(Color.WHITE);
        repaint();
    }
}
