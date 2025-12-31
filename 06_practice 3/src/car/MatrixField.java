package car;

/**
 * 公共矩阵接口，便于多个 FieldMatrix 实现之间切换。
 */
public interface MatrixField {
    enum CellState { EMPTY, CAR, WALL }

    CellState getCellState(int r, int c);

    boolean addWall(int r, int c);

    boolean removeWall(int r, int c);

    boolean moveCarTo(int fr, int fc, int tr, int tc);

    Position occupyFirstFreeCellByCar();

    int getRows();

    int getCols();
}
