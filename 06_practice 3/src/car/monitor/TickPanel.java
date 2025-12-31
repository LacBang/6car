package car.monitor;

import javax.swing.*;
import java.awt.*;

/**
 * 在主窗口旁边显示 tick 事件。
 */
public class TickPanel extends JPanel implements TickListener {
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private int maxEntries = 200;

    public TickPanel(){
        super(new BorderLayout());
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(list);
        add(scrollPane, BorderLayout.CENTER);
        add(new JLabel("Tick 监听 / 伪代码执行"), BorderLayout.NORTH);
    }

    @Override
    public void onTick(TickEvent event) {
        SwingUtilities.invokeLater(() -> {
            model.addElement(event.toString());
            while (model.size() > maxEntries){
                model.remove(0);
            }
            list.ensureIndexIsVisible(model.size()-1);
        });
    }
}
