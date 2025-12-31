package car;

import car.monitor.MonitorCenter;
import car.monitor.TickType;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 两把锁：按线性顺序锁住“起点+终点”两格，演示避免死锁的排序策略。
 */
public class OrderedPairMatrixField implements MatrixField {
    private final CellState[][] cells;
    private final ReentrantLock[][] locks;
    private final int rows;
    private final int cols;

    public OrderedPairMatrixField(int rows, int cols){
        this.rows = rows;
        this.cols = cols;
        this.cells = new CellState[rows][cols];
        this.locks = new ReentrantLock[rows][cols];
        for (int r=0;r<rows;r++){
            for (int c=0;c<cols;c++){
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

    private boolean inBounds(int r,int c){
        return r>=0 && r<rows && c>=0 && c<cols;
    }

    private int lin(int r,int c){
        return r*cols + c;
    }

    private void lockPairOrdered(int r1,int c1,int r2,int c2){
        int a = lin(r1,c1), b = lin(r2,c2);
        if (a <= b){
            locks[r1][c1].lock();
            if (a != b) locks[r2][c2].lock();
        }else{
            locks[r2][c2].lock();
            locks[r1][c1].lock();
        }
    }
    private void unlockPairOrdered(int r1,int c1,int r2,int c2){
        int a = lin(r1,c1), b = lin(r2,c2);
        if (a <= b){
            if (a != b) locks[r2][c2].unlock();
            locks[r1][c1].unlock();
        }else{
            locks[r1][c1].unlock();
            locks[r2][c2].unlock();
        }
    }

    @Override
    public CellState getCellState(int r, int c) {
        if (!inBounds(r,c)) throw new IndexOutOfBoundsException();
        locks[r][c].lock();
        try{
            return cells[r][c];
        }finally {
            locks[r][c].unlock();
        }
    }

    @Override
    public boolean addWall(int r, int c) {
        if (!inBounds(r,c)) return false;
        locks[r][c].lock();
        try{
            if (cells[r][c] == CellState.EMPTY){
                cells[r][c] = CellState.WALL;
                return true;
            }
            return false;
        }finally {
            locks[r][c].unlock();
        }
    }

    @Override
    public boolean removeWall(int r, int c) {
        if (!inBounds(r,c)) return false;
        locks[r][c].lock();
        try{
            if (cells[r][c] == CellState.WALL){
                cells[r][c] = CellState.EMPTY;
                return true;
            }
            return false;
        }finally {
            locks[r][c].unlock();
        }
    }

    @Override
    public boolean moveCarTo(int fr, int fc, int tr, int tc) {
        MonitorCenter.tick(TickType.BEHAVIOR_START,"[ORDERED] move "+fr+","+fc+" -> "+tr+","+tc);
        if (!inBounds(fr,fc) || !inBounds(tr,tc)) return false;
        if (fr==tr && fc==tc) return true;
        lockPairOrdered(fr,fc,tr,tc);
        try{
            if (cells[fr][fc] != CellState.CAR) return false;
            if (cells[tr][tc] != CellState.EMPTY) return false;
            cells[fr][fc] = CellState.EMPTY;
            cells[tr][tc] = CellState.CAR;
            MonitorCenter.tick(TickType.BEHAVIOR_END,"[ORDERED] moved");
            return true;
        }finally {
            unlockPairOrdered(fr,fc,tr,tc);
        }
    }

    @Override
    public Position occupyFirstFreeCellByCar() {
        for (int r=0;r<rows;r++){
            for (int c=0;c<cols;c++){
                locks[r][c].lock();
                try{
                    MonitorCenter.tick(TickType.ATOMIC,"[ORDERED] occupy "+r+","+c);
                    if (cells[r][c] == CellState.EMPTY){
                        cells[r][c] = CellState.CAR;
                        return new Position(r,c);
                    }
                }finally {
                    locks[r][c].unlock();
                }
            }
        }
        throw new RuntimeException("No empty fields!");
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
