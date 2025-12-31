package car;

import car.monitor.MonitorCenter;
import car.monitor.TickType;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 全局一把锁：最简单的同步方式，适合演示阻塞与串行化。
 */
public class GlobalLockMatrixField implements MatrixField {
    private final CellState[][] cells;
    private final ReentrantLock global = new ReentrantLock();
    private final int rows;
    private final int cols;

    public GlobalLockMatrixField(int rows, int cols){
        this.rows = rows;
        this.cols = cols;
        this.cells = new CellState[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = CellState.EMPTY;
            }
        }
    }

    public void copyFrom(CellState[][] src){
        for (int r=0;r<rows;r++){
            System.arraycopy(src[r],0,cells[r],0,cols);
        }
    }

    private boolean inBounds(int r,int c){
        return r>=0 && r<rows && c>=0 && c<cols;
    }

    @Override
    public CellState getCellState(int r, int c) {
        global.lock();
        try {
            if (!inBounds(r,c)) throw new IndexOutOfBoundsException();
            return cells[r][c];
        } finally {
            global.unlock();
        }
    }

    @Override
    public boolean addWall(int r, int c) {
        global.lock();
        try{
            if (!inBounds(r,c)) return false;
            if (cells[r][c] == CellState.EMPTY){
                cells[r][c] = CellState.WALL;
                return true;
            }
            return false;
        }finally {
            global.unlock();
        }
    }

    @Override
    public boolean removeWall(int r, int c) {
        global.lock();
        try{
            if (!inBounds(r,c)) return false;
            if (cells[r][c] == CellState.WALL){
                cells[r][c] = CellState.EMPTY;
                return true;
            }
            return false;
        }finally {
            global.unlock();
        }
    }

    @Override
    public boolean moveCarTo(int fr, int fc, int tr, int tc) {
        MonitorCenter.tick(TickType.ATOMIC,"[GLOBAL] tryLock target "+tr+","+tc, tr, tc);
        waitLock(global);
        try{
            if (!inBounds(fr,fc) || !inBounds(tr,tc)) return false;
            if (fr==tr && fc==tc) return true;
            MonitorCenter.tick(TickType.ATOMIC,"[GLOBAL] check from "+fr+","+fc, fr, fc);
            if (cells[fr][fc] != CellState.CAR) return false;
            MonitorCenter.tick(TickType.ATOMIC,"[GLOBAL] check target "+tr+","+tc, tr, tc);
            if (cells[tr][tc] != CellState.EMPTY) {
                MonitorCenter.tick(TickType.CRITICAL,"[GLOBAL] 吞并 "+tr+","+tc, tr, tc);
                return false;
            }
            cells[fr][fc] = CellState.EMPTY;
            cells[tr][tc] = CellState.CAR;
            MonitorCenter.tick(TickType.ATOMIC,"[GLOBAL] write from "+fr+","+fc, fr, fc);
            MonitorCenter.tick(TickType.ATOMIC,"[GLOBAL] write target "+tr+","+tc, tr, tc);
            MonitorCenter.tick(TickType.BEHAVIOR_END,"[GLOBAL] moved", tr, tc);
            return true;
        }finally {
            MonitorCenter.tick(TickType.ATOMIC,"[GLOBAL] unlock "+tr+","+tc, tr, tc);
            global.unlock();
        }
    }

    @Override
    public Position occupyFirstFreeCellByCar() {
        waitLock(global);
        try{
            for (int r=0;r<rows;r++){
                for (int c=0;c<cols;c++){
                    MonitorCenter.tick(TickType.ATOMIC,"[GLOBAL] occupy "+r+","+c);
                    if (cells[r][c] == CellState.EMPTY){
                        cells[r][c] = CellState.CAR;
                        return new Position(r,c);
                    }
                }
            }
            throw new RuntimeException("No empty fields!");
        }finally {
            global.unlock();
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

    private void waiting(boolean w){
        Integer id = MonitorCenter.currentCarId();
        if (id != null){
            MonitorCenter.updateWaiting(id, w);
        }
    }

    private void waitLock(ReentrantLock lock){
        waiting(true);
        try{
            while(!lock.tryLock()){
                try{ Thread.sleep(5);}catch(InterruptedException e){ Thread.currentThread().interrupt(); }
            }
        }finally {
            waiting(false);
        }
    }
}
