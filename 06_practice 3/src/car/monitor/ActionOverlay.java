package car.monitor;

import car.Position;

public class ActionOverlay {
    public final Position from;
    public final Position to;
    public final String description;

    public ActionOverlay(Position from, Position to, String description) {
        this.from = from;
        this.to = to;
        this.description = description;
    }
}
