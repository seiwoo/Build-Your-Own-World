package core;

public class Hallway {
    public static final int WIDTH = 1;
    private Room from;
    private Room to;
    private int distance;
    public Hallway(Room from, Room to, int distance) {
        this.from = from;
        this.to = to;
        this.distance = distance;
    }

    public Room getFrom() {
        return from;
    }

    public Room getTo() {
        return to;
    }

    public int getDistance() {
        return distance;
    }
}
