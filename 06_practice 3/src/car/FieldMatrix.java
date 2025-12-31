package car;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class FieldMatrix {
    enum CellState { EMPTY, CAR, WALL }

    private final CellState[][] cells;
    private final ReentrantLock[][] locks; // 每个格子一个独立的可重入锁

    public final int rows;
    public final int cols;

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
                        //System.out.println("line=" + line + " i=" + i + " j=" + j);
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

    //original
//    public synchronized Position occupyFirstFreeCellByCar(){
//        for (int i = 0; i < rows; i++)
//            for (int j = 0; j < cols; j++)
//                if (cells[i][j] == CellState.EMPTY) {
//                    cells[i][j] = CellState.CAR;
//                    return new Position(i, j);
//                }
//
//        throw new RuntimeException("No empty fields!");
//    }
    public Position occupyFirstFreeCellByCar() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 尝试只锁这个格子，避免全局阻塞与死锁
                if (locks[i][j].tryLock()) {
                    try {
                        if (cells[i][j] == CellState.EMPTY) {
                            cells[i][j] = CellState.CAR;
                            return new Position(i, j);
                        }
                    } finally {
                        locks[i][j].unlock();
                    }
                }
                // tryLock 失败说明该格当前在被修改，跳过继续找下一个
            }
        }
        throw new RuntimeException("No empty fields!");
    }

    /* ===== 工具方法 ===== */
    private boolean inBounds(int r, int c){
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }
    //private int lin(int r, int c){ return r * cols + c; }

    private void lockCell(int r, int c){
        locks[r][c].lock();
    }
    private void unlockCell(int r, int c){
        locks[r][c].unlock();
    }

//    private void lockPairOrdered(int r1, int c1, int r2, int c2){
//        int a = lin(r1, c1), b = lin(r2, c2);
//        if (a <= b) { locks[r1][c1].lock(); if (a != b) locks[r2][c2].lock(); }
//        else        { locks[r2][c2].lock(); locks[r1][c1].lock(); }
//    }
//    private void unlockPairOrdered(int r1, int c1, int r2, int c2){
//        int a = lin(r1, c1), b = lin(r2, c2);
//        if (a <= b) { if (a != b) locks[r2][c2].unlock(); locks[r1][c1].unlock(); }
//        else        { locks[r1][c1].unlock(); locks[r2][c2].unlock(); }
//    }

    /* ===== 读：建议也加锁，避免读到中间态 ===== */
    public CellState getCellState(int r, int c){
        if (!inBounds(r,c)) throw new IndexOutOfBoundsException();
        lockCell(r,c);
        try {
            return cells[r][c];
        } finally {
            unlockCell(r,c);
        }
    }

    /* ===== 单格写：加墙/拆墙（绝不覆盖车） ===== */
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

    /* ===== 双格写：移动车（原子：检查+写入） ===== */
    public boolean moveCarTo(int fr, int fc, int tr, int tc){
        if (!inBounds(fr,fc) || !inBounds(tr,tc)) return false;
        // 同一格的情况（原地不动）
        if (fr == tr && fc == tc) return true;

        lockCell(tr, tc);
        try {
            if (cells[fr][fc] != CellState.CAR)   return false;
            if (cells[tr][tc] != CellState.EMPTY) return false;

            // 原子更新
            cells[fr][fc] = CellState.EMPTY;
            cells[tr][tc] = CellState.CAR;
            return true;
        } finally {
            unlockCell(tr, tc);
        }
    }


}
