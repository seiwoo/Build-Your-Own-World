package core;
public class Room {
    public static final int CENTER = 2;
    public static final int WIDTH_MAKER = 10;
    public static final int HEIGHT_MAKER = 10;
    int ROOM_WIDTH;
    int ROOM_HEIGHT;
    int[] center;
    int x;
    int y;
    public Room(int widthCode, int heightCode, int x, int y) {
        ROOM_WIDTH = Math.floorMod(widthCode, World.WIDTH / WIDTH_MAKER) + World.WIDTH / WIDTH_MAKER;
        ROOM_HEIGHT = Math.floorMod(heightCode, World.HEIGHT / HEIGHT_MAKER) + World.HEIGHT / HEIGHT_MAKER;

        center = new int[2];
        this.x = x;
        this.y = y;
        center[0] = x + ROOM_WIDTH / CENTER;
        center[1] = y + ROOM_HEIGHT / CENTER;
    }
}
