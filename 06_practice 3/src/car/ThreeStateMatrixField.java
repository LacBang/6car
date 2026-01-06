package car;

import car.monitor.MonitorCenter;
import car.monitor.TickType;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 三把锁：按 EMPTY/CAR/WALL 分类锁，演示“资源类型”粒度的同步。
 */
public class ThreeStateMatrixField implements MatrixField {
    private final CellState[][] cells;
    private final ReentrantLock emptyLock = new ReentrantLock();
    private final ReentrantLock carLock = new ReentrantLock();
    private final ReentrantLock wallLock = new ReentrantLock();
    private final int rows;
    private final int cols;

    public ThreeStateMatrixField(int rows, int cols){
        this.rows = rows;
        this.cols = cols;
        this.cells = new CellState[rows][cols];
        for (int r=0;r<rows;r++){
            for (int c=0;c<cols;c++){
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

    private ReentrantLock lockFor(CellState state){
        switch (state){
            case WALL: return wallLock;
            case CAR: return carLock;
            default: return emptyLock;
        }
    }

    private void lockPair(ReentrantLock a, ReentrantLock b){
        if (a == b){
            a.lock();
            return;
        }
        // 固定顺序避免死锁
        int ha = System.identityHashCode(a);
        int hb = System.identityHashCode(b);
        if (ha < hb){
            a.lock(); b.lock();
        }else if (ha > hb){
            b.lock(); a.lock();
        }else{
            synchronized (this){
                a.lock(); b.lock();
            }
        }
    }
    private void unlockPair(ReentrantLock a, ReentrantLock b){
        if (a == b){
            a.unlock();
            return;
        }
        a.unlock(); b.unlock();
    }

    @Override
    public CellState getCellState(int r, int c) {
        if (!inBounds(r,c)) throw new IndexOutOfBoundsException();
        ReentrantLock lock = lockFor(cells[r][c]);
        lock.lock();
        try{
            return cells[r][c];
        }finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addWall(int r, int c) {
        if (!inBounds(r,c)) return false;
        lockPair(emptyLock, wallLock);
        try{
            if (cells[r][c] == CellState.EMPTY){
                cells[r][c] = CellState.WALL;
                return true;
            }
            return false;
        }finally {
            unlockPair(emptyLock, wallLock);
        }
    }

    @Override
    public boolean removeWall(int r, int c) {
        if (!inBounds(r,c)) return false;
        lockPair(emptyLock, wallLock);
        try{
            if (cells[r][c] == CellState.WALL){
                cells[r][c] = CellState.EMPTY;
                return true;
            }
            return false;
        }finally {
            unlockPair(emptyLock, wallLock);
        }
    }

    @Override
    public boolean moveCarTo(int fr, int fc, int tr, int tc) {
        MonitorCenter.tick(TickType.ATOMIC,"[THREE] tryLock "+fr+","+fc+" -> "+tr+","+tc, tr, tc);
        if (!inBounds(fr,fc) || !inBounds(tr,tc)) return false;
        if (fr==tr && fc==tc) return true;
        waitPair(carLock, emptyLock);
        try{
            MonitorCenter.tick(TickType.ATOMIC,"[THREE] check from "+fr+","+fc, fr, fc);
            if (cells[fr][fc] != CellState.CAR) return false;
            MonitorCenter.tick(TickType.ATOMIC,"[THREE] check target "+tr+","+tc, tr, tc);
            if (cells[tr][tc] != CellState.EMPTY) {
                MonitorCenter.tick(TickType.CRITICAL,"[THREE] 吞并 "+tr+","+tc, tr, tc);
                return false;
            }
            cells[fr][fc] = CellState.EMPTY;
            cells[tr][tc] = CellState.CAR;
            MonitorCenter.tick(TickType.ATOMIC,"[THREE] write from "+fr+","+fc, fr, fc);
            MonitorCenter.tick(TickType.ATOMIC,"[THREE] write target "+tr+","+tc, tr, tc);
            MonitorCenter.tick(TickType.BEHAVIOR_END,"[THREE] moved", tr, tc);
            return true;
        }finally {
            MonitorCenter.tick(TickType.ATOMIC,"[THREE] unlock pair", tr, tc);
            unlockPair(carLock, emptyLock);
        }
    }

    @Override
    public Position occupyFirstFreeCellByCar() {
        waitPair(emptyLock, carLock);
        try{
            for (int r=0;r<rows;r++){
                for (int c=0;c<cols;c++){
                    MonitorCenter.tick(TickType.ATOMIC,"[THREE] occupy "+r+","+c);
                    if (cells[r][c] == CellState.EMPTY){
                        cells[r][c] = CellState.CAR;
                        return new Position(r,c);
                    }
                }
            }
        }finally {
            unlockPair(emptyLock, carLock);
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

    private void waiting(boolean w){
        Integer id = MonitorCenter.currentCarId();
        if (id != null){
            MonitorCenter.updateWaiting(id, w);
        }
    }

    private void waitPair(ReentrantLock a, ReentrantLock b){
        waiting(true);
        try{
            while(true){
                boolean gotA = a.tryLock();
                boolean gotB = b.tryLock();
                if (gotA && gotB) break;
                if (gotA) a.unlock();
                if (gotB) b.unlock();
                try{ Thread.sleep(5);}catch(InterruptedException e){ Thread.currentThread().interrupt(); }
            }
        }finally {
            waiting(false);
        }
    }
}
