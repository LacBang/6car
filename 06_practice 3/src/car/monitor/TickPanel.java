package car.monitor;

import javax.swing.*;
import java.awt.*;
import car.monitor.MonitorCenter;

/**
 * 在主窗口旁边显示 tick 事件。
 */
public class TickPanel extends JPanel implements TickListener {
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private int page = 0;

    public TickPanel(){
        super(new BorderLayout());
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        setPreferredSize(new Dimension(300, 300));
        JScrollPane scrollPane = new JScrollPane(list);
        add(scrollPane, BorderLayout.CENTER);
        add(new JLabel("原子行为监视（每2帧一页，前->后）"), BorderLayout.NORTH);
    }

    public void setPageByFrame(int frameIndex){
        int newPage = frameIndex / 2;
        if (newPage != page){
            page = newPage;
            SwingUtilities.invokeLater(() -> {
                model.clear();
                for (TickEvent event : MonitorCenter.getLog()){
                    if (event.getFrameIndex() / 2 == page){
                        model.addElement(event.toString());
                    }
                }
            });
        }
    }

    @Override
    public void onTick(TickEvent event) {
        int eventPage = event.getFrameIndex() / 2;
        if (eventPage != page) return;
        SwingUtilities.invokeLater(() -> {
            model.addElement(event.toString());
            list.ensureIndexIsVisible(model.size()-1);
        });
    }
}
