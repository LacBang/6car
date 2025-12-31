package car;

import java.util.ArrayList;
import java.util.List;

public class BasicCarServer implements CarServer {
    protected final FieldMatrix fieldMatrix;
    protected final List<Car> cars;
    protected final CarEventsListener carEventsListener;

    protected BasicCarServer(FieldMatrix fieldMatrix, CarEventsListener carEventsListener){
        cars = new ArrayList<>();
        this.fieldMatrix = fieldMatrix;
        this.carEventsListener = carEventsListener;
    }

    @Override
    public Car createCar() {
        Position freeCell = fieldMatrix.occupyFirstFreeCellByCar();
        Car car = new Car(this, freeCell);
        cars.add(car);
        carEventsListener.carCreated(car);
        return car;
    }

    @Override
    public void destroyCar(Car car) {
        cars.remove(car);
        carEventsListener.carDestroyed(car);
    }

    @Override
    public boolean moveCarTo(Car car, Direction direction) {
        Position from = car.getPosition();
        Position to = from.move(direction);
        boolean ret = fieldMatrix.moveCarTo(from.row, from.col, to.row, to.col);
        carEventsListener.carMoved(car,from,to,ret);
        return ret;
    }

    public Runnable wallTask(){
        return new WallRandomizer();
    }

    private class WallRandomizer implements Runnable{
        private final java.util.Random rnd = new java.util.Random();
        @Override
        public void run(){
            while(true){
                try{ Thread.sleep(300 + rnd.nextInt(700)); }catch(InterruptedException e){ break; }
                if (rnd.nextBoolean()){
                    // 尝试在空格子加墙（不会覆盖车，因为 addWall 只允许 EMPTY->WALL）
                    for (int t=0; t<20; t++){
                        int r = rnd.nextInt(fieldMatrix.rows);
                        int c = rnd.nextInt(fieldMatrix.cols);
                        if (fieldMatrix.addWall(r,c)) break;
                    }
                }else{
                    // 尝试随机拆墙
                    for (int t=0; t<20; t++){
                        int r = rnd.nextInt(fieldMatrix.rows);
                        int c = rnd.nextInt(fieldMatrix.cols);
                        if (fieldMatrix.removeWall(r,c)) break;
                    }
                }
                carEventsListener.fieldChanged();
            }
        }
    }
}
