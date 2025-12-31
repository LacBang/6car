package car.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * tick 总线：记录事件并推送给 UI。
 */
public final class MonitorCenter {
    private static final List<TickListener> listeners = new CopyOnWriteArrayList<>();
    private static final List<ThreadStateListener> threadListeners = new CopyOnWriteArrayList<>();
    private static final List<TickEvent> log = new CopyOnWriteArrayList<>();
    private static final AtomicInteger seq = new AtomicInteger(1);
    private static final Map<Integer, Thread.State> threadStates = new ConcurrentHashMap<>();
    private static final File logDir = new File("out/monitor");

    private MonitorCenter(){}

    public static void addListener(TickListener listener){
        listeners.add(listener);
    }

    public static void addThreadListener(ThreadStateListener listener){
        threadListeners.add(listener);
    }

    public static List<TickEvent> getLog(){
        return log;
    }

    public static void tick(TickType type, String message){
        int index = seq.getAndIncrement();
        TickEvent event = new TickEvent(System.nanoTime(), type, message, Thread.currentThread().getName(), index);
        log.add(event);
        for (TickListener listener : listeners){
            listener.onTick(event);
        }
        appendToFile(event);
    }

    public static void updateThreadState(int carId, Thread.State state){
        threadStates.put(carId, state);
        for (ThreadStateListener listener : threadListeners){
            listener.onThreadState(carId, state);
        }
        tick(TickType.THREAD,"car-"+carId+" -> "+state);
    }

    public static Map<Integer, Thread.State> snapshotThreadStates(){
        return new ConcurrentHashMap<>(threadStates);
    }

    private static void appendToFile(TickEvent event){
        if (!logDir.exists()){
            //noinspection ResultOfMethodCallIgnored
            logDir.mkdirs();
        }
        File logFile = new File(logDir, "ticks-"+ LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)+".log");
        try(FileWriter writer = new FileWriter(logFile,true)){
            writer.write(event.toString());
            writer.write(System.lineSeparator());
        }catch(IOException ignored){}
    }
}
