package car.monitor;

import car.MatrixField;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 显示“一对二”前后态：上一帧 vs 当前帧，带方向箭头。
 */
public class ActionPreviewPanel extends JPanel {
    private PlaybackFrame prev;
    private PlaybackFrame curr;
    private final int cellSize = 10;

    public ActionPreviewPanel(){
        setPreferredSize(new Dimension(280, 140));
        setBorder(BorderFactory.createTitledBorder("前后态 (前 -> 后)"));
    }

    public void update(List<PlaybackFrame> frames){
        if (frames == null || frames.size() < 1) return;
        int last = frames.size()-1;
        curr = frames.get(last);
        prev = last > 0 ? frames.get(last-1) : null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int top = 20;
        int left = 10;
        drawFrame(g, prev, left, top, "前");
        drawFrame(g, curr, left + 120, top, "后");
        g.setColor(Color.BLACK);
        g.drawString("→", left + 110, top + 50);
    }

    private void drawFrame(Graphics g, PlaybackFrame frame, int x, int y, String label){
        g.setColor(Color.BLACK);
        g.drawString(label, x, y-5);
        if (frame == null) return;
        MatrixField.CellState[][] snap = frame.getSnapshot();
        int rows = snap.length;
        int cols = snap[0].length;
        int size = cellSize;
        for (int r=0;r<rows && r<8;r++){
            for (int c=0;c<cols && c<8;c++){
                switch (snap[r][c]){
                    case WALL -> g.setColor(Color.RED);
                    case CAR -> g.setColor(Color.BLUE);
                    default -> g.setColor(Color.LIGHT_GRAY);
                }
                g.fillRect(x + c*size, y + r*size, size, size);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x + c*size, y + r*size, size, size);
            }
        }
    }
}
