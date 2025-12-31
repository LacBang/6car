package car;

import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * 根据 LockMode 选择不同的矩阵实现，并负责加载初始布局。
 */
public final class MatrixFieldFactory {
    private MatrixFieldFactory(){}

    public static MatrixField load(LockMode mode, InputStreamReader isr){
        try (Scanner scanner = new Scanner(isr)){
            int rows = 0;
            int cols = 0;
            if (scanner.hasNextInt()) rows = scanner.nextInt();
            if (scanner.hasNextInt()) cols = scanner.nextInt();
            scanner.nextLine();

            MatrixField.CellState[][] initial = new MatrixField.CellState[rows][cols];
            for (int r=0;r<rows;r++){
                for (int c=0;c<cols;c++){
                    initial[r][c] = MatrixField.CellState.EMPTY;
                }
            }
            for (int i = 0; i < rows; i++) {
                String line = "";
                if (scanner.hasNext()) line = scanner.nextLine();
                for (int j = 0; j < Math.min(line.length(), cols); j++) {
                    if (line.charAt(j) == '*'){
                        initial[i][j] = MatrixField.CellState.WALL;
                    }
                }
            }
            return build(mode, rows, cols, initial);
        }
    }

    public static MatrixField build(LockMode mode, int rows, int cols, MatrixField.CellState[][] initial){
        MatrixField field;
        switch (mode){
            case GLOBAL_SINGLE:
                field = new GlobalLockMatrixField(rows, cols);
                ((GlobalLockMatrixField) field).copyFrom(initial);
                break;
            case THREE_STATE:
                field = new ThreeStateMatrixField(rows, cols);
                ((ThreeStateMatrixField) field).copyFrom(initial);
                break;
            case ORDERED_PAIR:
                field = new OrderedPairMatrixField(rows, cols);
                ((OrderedPairMatrixField) field).copyFrom(initial);
                break;
            case TARGET_SINGLE:
            default:
                field = new FieldMatrix(rows, cols);
                ((FieldMatrix) field).copyFrom(initial);
                break;
        }
        return field;
    }
}
