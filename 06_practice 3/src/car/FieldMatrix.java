package car;

import car.monitor.MonitorCenter;
import car.monitor.TickType;

import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 目标格子单锁策略。保留原始逻辑，便于和其他实现做横向对比。
 */
public class FieldMatrix implements MatrixField {
    private final CellState[][] cells;
    private final ReentrantLock[][] locks; // 每个格子一个独立的可重入锁

    private final int rows;
    private final int cols;

    public FieldMatrix(int rows, int cols){
        this.rows = rows;
        this.cols = cols;
        this.cells = new CellState[rows][cols];
        this.locks = new ReentrantLock[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = CellState.EMPTY;
                locks[r][c] = new ReentrantLock();
            }
        }
    }

    public void copyFrom(CellState[][] src){
        for (int r=0;r<rows;r++){
            System.arraycopy(src[r],0,cells[r],0,cols);
        }
    }

    public static FieldMatrix load(InputStreamReader isr){
        try (Scanner scanner = new Scanner(isr)){
            int rows = 0;
            int cols = 0;
            if (scanner.hasNextInt()) rows = scanner.nextInt();
            if (scanner.hasNextInt()) cols = scanner.nextInt();

            FieldMatrix fm = new FieldMatrix(rows,cols);
            scanner.nextLine();

            for (int i = 0; i < rows; i++) {
                String line = "";
                if (scanner.hasNext()) line = scanner.nextLine();
                try {
                    for (int j = 0; j < cols; j++) {
                        switch (line.charAt(j)) {
                            case '*':
                                fm.cells[i][j] = CellState.WALL;
                        }
                    }
                }catch(StringIndexOutOfBoundsException e){}
            }
            return fm;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Position occupyFirstFreeCellByCar() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                MonitorCenter.tick(TickType.ATOMIC,"try occupy "+i+","+j);
                // 尝试只锁这个格子，避免全局阻塞与死锁
                if (locks[i][j].tryLock()) {
                    try {
                        if (cells[i][j] == CellState.EMPTY) {
                            cells[i][j] = CellState.CAR;
                            MonitorCenter.tick(TickType.CRITICAL,"occupy success "+i+","+j);
                            return new Position(i, j);
                        }
                    } finally {
                        locks[i][j].unlock();
                    }
                }
            }
        }
        throw new RuntimeException("No empty fields!");
    }

    private boolean inBounds(int r, int c){
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    private void lockCell(int r, int c){
        locks[r][c].lock();
    }
    private void unlockCell(int r, int c){
        locks[r][c].unlock();
    }

    @Override
    public CellState getCellState(int r, int c){
        if (!inBounds(r,c)) throw new IndexOutOfBoundsException();
        lockCell(r,c);
        try {
            return cells[r][c];
        } finally {
            unlockCell(r,c);
        }
    }

    @Override
    public boolean addWall(int r, int c){
        if (!inBounds(r,c)) return false;
        lockCell(r,c);
        try{
            if (cells[r][c] == CellState.EMPTY){
                cells[r][c] = CellState.WALL;
                return true;
            }
            return false;
        } finally {
            unlockCell(r,c);
        }
    }

    @Override
    public boolean removeWall(int r, int c){
        if (!inBounds(r,c)) return false;
        lockCell(r,c);
        try{
            if (cells[r][c] == CellState.WALL){
                cells[r][c] = CellState.EMPTY;
                return true;
            }
            return false;
        } finally {
            unlockCell(r,c);
        }
    }

    @Override
    public boolean moveCarTo(int fr, int fc, int tr, int tc){
        MonitorCenter.tick(TickType.BEHAVIOR_START,"move "+fr+","+fc+" -> "+tr+","+tc);
        if (!inBounds(fr,fc) || !inBounds(tr,tc)) return false;
        if (fr == tr && fc == tc) return true;

        lockCell(tr, tc);
        try {
            if (cells[fr][fc] != CellState.CAR)   return false;
            if (cells[tr][tc] != CellState.EMPTY) return false;

            cells[fr][fc] = CellState.EMPTY;
            cells[tr][tc] = CellState.CAR;
            MonitorCenter.tick(TickType.BEHAVIOR_END,"move done "+fr+","+fc+" -> "+tr+","+tc);
            return true;
        } finally {
            unlockCell(tr, tc);
        }
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public int getCols() {
        return cols;
    }
}
