package car.monitor;

import car.Position;

import java.awt.*;

public class CarSnapshot {
    public final int id;
    public final String name;
    public final Color color;
    public final Position position;

    public CarSnapshot(int id, String name, Color color, Position position) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.position = position;
    }
}
