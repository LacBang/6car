package car.monitor;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 显示所有小车线程状态。
 */
public class ThreadStatusWindow extends JFrame implements ThreadStateListener {
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final Map<Integer, Thread.State> states = new ConcurrentHashMap<>();

    public ThreadStatusWindow(){
        super("线程状态监视");
        setSize(240,400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        JList<String> list = new JList<>(model);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(list), BorderLayout.CENTER);
        setVisible(true);
    }

    @Override
    public void onThreadState(int carId, Thread.State state) {
        states.put(carId, state);
        SwingUtilities.invokeLater(this::refresh);
    }

    private void refresh(){
        model.clear();
        for (Map.Entry<Integer, Thread.State> entry : states.entrySet()){
            String line = "car-"+entry.getKey()+": "+entry.getValue();
            model.addElement(line);
        }
    }
}
