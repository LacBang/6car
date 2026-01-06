package car.monitor;

public interface ThreadStateListener {
    void onThreadState(int carId, Thread.State state);
}
