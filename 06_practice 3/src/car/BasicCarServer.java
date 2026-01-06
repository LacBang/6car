package car;

import car.monitor.MonitorCenter;
import car.monitor.TickType;

import java.util.ArrayList;
import java.util.List;

public class BasicCarServer implements CarServer {
    protected final MatrixField fieldMatrix;
    protected final List<Car> cars;
    protected final CarEventsListener carEventsListener;

    protected BasicCarServer(MatrixField fieldMatrix, CarEventsListener carEventsListener){
        cars = new ArrayList<>();
        this.fieldMatrix = fieldMatrix;
        this.carEventsListener = carEventsListener;
    }

    @Override
    public Car createCar() {
        MonitorCenter.beginActionFrame();
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
        MonitorCenter.beginActionFrame();
        Position from = car.getPosition();
        Position to = from.move(direction);
        MonitorCenter.bindCar(car.getIndex());
        MonitorCenter.tick(TickType.BEHAVIOR_START,"car-"+car.getIndex()+" try move "+direction);
        boolean ret;
        try{
            ret = fieldMatrix.moveCarTo(from.row, from.col, to.row, to.col);
            if (!ret){
                MonitorCenter.tick(TickType.CRITICAL,"car-"+car.getIndex()+" blocked at "+to, to.row, to.col);
            }
            carEventsListener.carMoved(car,from,to,ret);
            return ret;
        } finally {
            MonitorCenter.clearCar();
        }
    }

    public Runnable wallTask(){
        return new WallRandomizer();
    }

    private class WallRandomizer implements Runnable{
        private final java.util.Random rnd = new java.util.Random();
        @Override
        public void run(){
            while(true){
                try{
                    while (car.control.ControlCenter.isPaused()) Thread.sleep(100);
                    Thread.sleep(300 + rnd.nextInt(700));
                }catch(InterruptedException e){ break; }
                MonitorCenter.beginActionFrame();
                if (rnd.nextBoolean()){
                    for (int t=0; t<20; t++){
                        int r = rnd.nextInt(fieldMatrix.getRows());
                        int c = rnd.nextInt(fieldMatrix.getCols());
                        if (fieldMatrix.addWall(r,c)) break;
                    }
                }else{
                    for (int t=0; t<20; t++){
                        int r = rnd.nextInt(fieldMatrix.getRows());
                        int c = rnd.nextInt(fieldMatrix.getCols());
                        if (fieldMatrix.removeWall(r,c)) break;
                    }
                }
                carEventsListener.fieldChanged();
            }
        }
    }
}
