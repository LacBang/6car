package car;

import car.command.*;
import car.monitor.MonitorCenter;
import car.monitor.ThreadLightWindow;

import java.awt.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;


/**
 * @author : Alex
 **/
public class Main {
    public static final LockMode MODE = LockMode.TARGET_SINGLE;

    public static void main(String[] args) throws Exception{
        InputStream is = CarPainter.class.getClassLoader().getResourceAsStream("field.txt");
        MatrixField fm = MatrixFieldFactory.load(MODE, new InputStreamReader(is));
        CarPainter p = new CarPainter(fm);
        ThreadLightWindow lightWindow = new ThreadLightWindow();
        MonitorCenter.addThreadListener(lightWindow);
        MonitorCenter.addWaitingListener(lightWindow);
        BasicCarServer carServer = new BasicCarServer(fm, p);
        new Thread(carServer.wallTask()).start(); // ////
        //Car car = carServer.createCar();

//        is = CarPainter.class.getClassLoader().getResourceAsStream("script.txt");
//        Script script = Script.load(new InputStreamReader(is), car);
//        script.execute();

        class CarMover implements Runnable{
            private final String name;

            public CarMover(String name){
                this.name = name;
            }
            @Override
            public void run(){
                Random random = new Random();
                Car car = carServer.createCar();
                car.setName(name);
                Thread.currentThread().setName("car-thread-"+car.getIndex()+"-"+name);
                CarServer.Direction direction = CarServer.Direction.DOWN;
                while(true){
                    boolean result;
                    try {
                        result = car.moveTo(direction);
                    }catch(ArrayIndexOutOfBoundsException e){
                        result = false;
                    }
                    if (!result){
                        direction = CarServer.Direction.values()[random.nextInt(4)];
                    }
                }
            }
        }
        new Thread(new CarMover("Alex")).start(); //car1
        Thread.sleep(1000);
        new Thread(new CarMover("Petr")).start(); //car2
        Thread.sleep(1000);
        new Thread(new CarMover("Nata")).start(); //car3
        Thread.sleep(1000);
        new Thread(new CarMover("Boris")).start(); //car3

    }
}
