package car.monitor;

public interface WaitingLightListener {
    void onWaiting(int carId, boolean waiting);
}
