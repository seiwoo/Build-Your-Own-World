package core;

import tileengine.Tileset;
import tileengine.TETile;
import javax.sound.sampled.*;

import java.io.*;
import java.util.*;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;


public class World {
    static final int WIDTH = 70;
    static final int HEIGHT = 60;
    Random rand;
    int roomAmount;
    boolean[][] grid = new boolean[WIDTH][HEIGHT];
    ArrayList<Room> rooms = new ArrayList<>();
    TETile[][] world = new TETile[WIDTH][HEIGHT];
    public static final int MORE_ROOM = 6;
    public static final int ROOM_AT_LEAST = 10;
    public static final int MORE_HALLWAY = 3;
    public static final TETile OUT_SPACE = Tileset.NOTHING;
    public static final TETile FLOOR = Tileset.FLOOR;
    public static final TETile WALL = Tileset.WALL;
    public static final int MARGIN_WIDTH = WIDTH / 15;
    public static final int MARGIN_HEIGHT = HEIGHT / 15;
    public static class Position {
        int line;   // Remove final
        int column; // Remove final
        public Position(int line, int column) {
            this.line = line;
            this.column = column;
        }
    }

    private Position avatarPos;

    public long getSeed() {
        return rand == null ? 0 : rand.nextLong();
    }
    public Position getAvatarPosition() {
        return avatarPos;
    }
    public void setAvatarPosition(Position avatarPositionValue) {
        this.avatarPos = avatarPositionValue;
        updateAvatarPosition(avatarPositionValue);
    }

    private boolean lineOfSightEnabled = false;

    public void toggleLineOfSight() {
        lineOfSightEnabled = !lineOfSightEnabled;
    }
    public void walking(String soundFilePath)
            throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        InputStream audioSrc = getClass().getResourceAsStream(soundFilePath);
        if (audioSrc == null) {
            throw new FileNotFoundException("Sound file not found: " + soundFilePath);
        }
        InputStream bufferedIn = new BufferedInputStream(audioSrc);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
        Clip clip = AudioSystem.getClip();
        clip.open(audioStream);
        clip.start();
    }

    public boolean isTileVisible(int x, int y) {
        if (!lineOfSightEnabled) {
            return true;
        }
        int dx = Math.abs(x - avatarPos.line);
        int dy = Math.abs(y - avatarPos.column);
        int maxRange = 7;
        if (Math.sqrt(dx * dx + dy * dy) > maxRange) {
            return false;
        }
        return hasClearPath(avatarPos.line, avatarPos.column, x, y);
    }

    private boolean hasClearPath(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = Integer.signum(x2 - x1);
        int sy = Integer.signum(y2 - y1);
        int err = dx - dy;
        while (!(x1 == x2 && y1 == y2)) {
            if (world[x1][y1] == Tileset.WALL) {
                return false;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
        return world[x2][y2] != Tileset.WALL;
    }
    public static TETile fromCharacter(char c) {
        switch (c) {
            case '@': return Tileset.AVATAR;
            case '·': return Tileset.FLOOR;
            case '#': return Tileset.WALL;
            case ' ': return Tileset.NOTHING;
            default: throw new IllegalArgumentException("Invalid character: " + c);
        }
    }
    public static char toCharacter(TETile tile) {
        if (tile.equals(Tileset.AVATAR)) {
            return '@';
        }
        if (tile.equals(Tileset.FLOOR)) {
            return '·';
        }
        if (tile.equals(Tileset.WALL)) {
            return '#';
        }
        if (tile.equals(Tileset.NOTHING)) {
            return ' ';
        }
        throw new IllegalArgumentException("Invalid TETile: " + tile.description());
    }
    public TETile[][] generate(long seed) {
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                world[i][j] = OUT_SPACE;
                grid[i][j] = i <= MARGIN_WIDTH || i >= WIDTH - MARGIN_WIDTH
                        || j <= MARGIN_HEIGHT || j >= HEIGHT - MARGIN_HEIGHT;
            }
        }
        rand = new Random(seed);
        roomAmount = Math.floorMod(rand.nextInt(), MORE_ROOM) + ROOM_AT_LEAST;
        roomGenerator();
        hallwayGenerator();
        for (int i = 0; i < WIDTH; i++) {
            world[i][0] = WALL;
            world[i][HEIGHT - 1] = WALL;
        }
        for (int j = 0; j < HEIGHT; j++) {
            world[0][j] = WALL;
            world[WIDTH - 1][j] = WALL;
        }
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                if (world[i][j] == FLOOR) {
                    if ((i > 0 && world[i - 1][j] == OUT_SPACE)
                            || (i < WIDTH - 1 && world[i + 1][j] == OUT_SPACE)
                            || (j > 0 && world[i][j - 1] == OUT_SPACE)
                            || (j < HEIGHT - 1 && world[i][j + 1] == OUT_SPACE)) {
                        world[i][j] = WALL;
                    }
                }
            }
        }
        initiaizeAvatar();
        return world;
    }
    private void initiaizeAvatar() {
        for (int y = HEIGHT - 1; y >= 0; y--) {
            for (int x = 0; x < WIDTH; x++) {
                if (world[x][y] == Tileset.FLOOR) {
                    avatarPos = new Position(x, y);
                    world[x][y] = Tileset.AVATAR;
                    return;
                }
            }
        }
    }

    public void moveAvatar(char input) throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        Position newPos = new Position(avatarPos.line, avatarPos.column);
        switch (Character.toLowerCase(input)) {
            case 'w':
                newPos.column += 1;
                break;
            case 's':
                newPos.column -= 1;
                break;
            case 'a':
                newPos.line -= 1;
                break;
            case 'd':
                newPos.line += 1;
                break;
            default:
                System.out.println("Invalid input: " + input); // Log a message or handle error
                return;
        }
        if (isValidMove(newPos)) {
            walking("./step.wav");
            updateAvatarPosition(newPos);

        }
    }

    private boolean isValidMove(Position pos) {
        boolean withinBounds = pos.line >= 0 && pos.line < WIDTH && pos.column >= 0 && pos.column < HEIGHT;
        return withinBounds && world[pos.line][pos.column] != Tileset.WALL;
    }

    private void updateAvatarPosition(Position newPos) {
        world[avatarPos.line][avatarPos.column] = Tileset.FLOOR; // Clear previous avatar position
        avatarPos = newPos; // Update to new position
        world[avatarPos.line][avatarPos.column] = Tileset.AVATAR; // Set avatar in new position
    }



    public void roomGenerator() {

        generateRooms();

        populateWorldWithRooms();

        cleanUpWallArtifacts();

    }

    private void generateRooms() {

        while (roomAmount != 0) {

            int x = Math.floorMod(rand.nextInt(), WIDTH);

            int y = Math.floorMod(rand.nextInt(), HEIGHT);

            Room temp = new Room(rand.nextInt(), rand.nextInt(), x, y);

            if (isInvalidRoom(x, y, temp)) {

                continue;

            }

            if (!isSpaceAvailable(x, y, temp)) {

                continue;

            }

            rooms.add(temp);

            roomAmount--;

        }

    }

    private boolean isInvalidRoom(int x, int y, Room temp) {

        return x + temp.ROOM_WIDTH >= WIDTH || y + temp.ROOM_HEIGHT >= HEIGHT;

    }

    private boolean isSpaceAvailable(int x, int y, Room temp) {

        for (int w = x; w <= x + temp.ROOM_WIDTH; w++) {

            for (int h = y; h <= y + temp.ROOM_HEIGHT; h++) {

                if (grid[w][h]) {

                    return false;

                }

            }

        }

        return true;

    }

    private void populateWorldWithRooms() {

        for (Room current : rooms) {

            for (int row = current.x; row < current.x + current.ROOM_WIDTH; row++) {

                for (int col = current.y; col < current.y + current.ROOM_HEIGHT; col++) {

                    setupRoomTiles(current, row, col);

                }

            }

        }

    }

    private void setupRoomTiles(Room current, int row, int col) {

        List<Integer> borders = List.of(

                current.x,

                current.y,

                current.x + current.ROOM_WIDTH - 1,

                current.y + current.ROOM_HEIGHT - 1

        );

        if (borders.contains(row) || borders.contains(col)) {

            world[row][col] = WALL;

        } else {

            world[row][col] = FLOOR;

        }

        grid[row][col] = true;

    }

    private void cleanUpWallArtifacts() {

        for (Room current : rooms) {

            for (int row = current.x; row < current.x + current.ROOM_WIDTH; row++) {

                for (int col = current.y; col < current.y + current.ROOM_HEIGHT; col++) {

                    cleanupTile(row, col);

                }

            }

        }

    }

    private void cleanupTile(int row, int col) {

        int neighbors = countWallNeighbors(row, col);

        if ((world[row][col] == WALL && shouldConvertWallToFloor(row, col)) || neighbors > 3) {

            world[row][col] = FLOOR;

        }

    }

    private int countWallNeighbors(int row, int col) {

        int neighbors = 0;

        for (int r = -1; r <= 1; r++) {

            for (int c = -1; c <= 1; c++) {

                if (r == c && r == 0) {

                    continue;

                }

                if (isWithinBounds(row + r, col + c) && world[row + r][col + c] == WALL) {

                    neighbors++;

                }

            }

        }

        return neighbors;

    }

    private boolean shouldConvertWallToFloor(int row, int col) {

        return (world[row][col - 1] == FLOOR && world[row][col + 1] == FLOOR)

                || (world[row - 1][col] == FLOOR && world[row + 1][col] == FLOOR)

                || (world[row - 1][col - 1] == FLOOR && world[row + 1][col + 1] == FLOOR)

                || (world[row + 1][col - 1] == FLOOR && world[row - 1][col + 1] == FLOOR);

    }

    private boolean isWithinBounds(int row, int col) {

        return row >= 0 && row < world.length && col >= 0 && col < world[0].length;

    }

    public void hallwayGenerator() {
        List<Room> mark = new ArrayList<>();
        PriorityQueue<Hallway> allEdges = getAllEdges();

        mark.add(rooms.getFirst());
        Room first = rooms.getFirst();

        while (mark.size() != rooms.size()) {
            Room target = null;
            while (true) {
                Hallway h = allEdges.remove();
                if (h.getFrom().equals(first)) {
                    target = h.getTo();
                    break;
                }
            }
            Room closest = first;
            for (Room r : mark) {
                if (calculateDistance(r, target) < calculateDistance(r, closest)) {
                    closest = r;
                }
            }
            connectRooms(closest, target);
            mark.add(target);
        }
        for (int i = 0; i < rooms.size() / MORE_HALLWAY; i++) {
            int temp = rand.nextInt();
            int index = Math.floorMod(temp, rooms.size());
            int temp2 = rand.nextInt();
            int index2 = Math.floorMod(temp2, rooms.size());
            Room r1 = rooms.get(index);
            Room r2 = rooms.get(index2);
            connectRooms(r1, r2);
        }
    }
    public int calculateDistance(Room i, Room j) {
        double distance = sqrt(pow(i.center[0] - j.center[0], 2) + pow(i.center[1] - j.center[1], 2));
        return (int) distance;
    }

    public PriorityQueue<Hallway> getAllEdges() {
        PriorityQueue<Hallway> allEdges = new PriorityQueue<>(new HallwayGeneratorComparator());
        for (Room r: rooms) {
            for (Room R: rooms) {
                if (R.equals(r)) {
                    continue;
                }
                int distance = calculateDistance(r, R);
                allEdges.add(new Hallway(r, R, distance));
            }
        }
        return allEdges;
    }

    public static class HallwayGeneratorComparator implements Comparator<Hallway> {
        @Override
        public int compare(Hallway o1, Hallway o2) {
            return o1.getDistance() - o2.getDistance();
        }
    }

    public void connectRooms(Room r1, Room r2) {
        Room high;
        Room low;
        if (r1.y > r2.y) {
            low = r2;
            high = r1;
        } else {
            low = r1;
            high = r2;
        }
        int yMove = high.center[1] - low.center[1];

        for (int i = 1; i <= yMove; i++) {
            int x = low.center[0];
            int y = low.center[1] + i;
            if (x >= 0 && x < world.length && y >= 0 && y < world[x].length) {
                world[x][y] = FLOOR;
                grid[x][y] = true;
            }
            int wallY = low.center[1] + i;

            if (x >= 0 && x + 1 < world.length && wallY < world[x + 1].length) {
                if (!grid[x + 1][wallY]) {
                    world[x + 1][wallY] = WALL;
                    grid[x + 1][wallY] = true;
                }
            }
            if (x - 1 >= 0 && wallY < world[x - 1].length) {
                if (!grid[x - 1][wallY]) {
                    world[x - 1][wallY] = WALL;
                    grid[x - 1][wallY] = true;
                }
            }
        }
        connectRoomsHelper(r1, r2);
    }

    public void connectRoomsHelper(Room r1, Room r2) {
        Room right;
        Room left;
        Room low;
        if (r1.x > r2.x) {
            left = r2;
            right = r1;
        } else {
            left = r1;
            right = r2;
        }
        if (r1.y > r2.y) {
            low = r2;
        } else {
            low = r1;
        }
        int xMove = right.center[0] - left.center[0];

        if (!right.equals(low)) {
            for (int i = 1; i <= xMove; i++) {
                world[right.center[0] - i][right.center[1]] = FLOOR;
                grid[right.center[0] - i][right.center[1]] = true;
                int wallX = right.center[0] - i;
                if (!grid[wallX][right.center[1] + 1]) {
                    world[right.center[0] - i][right.center[1] + 1] = WALL;
                    grid[right.center[0] - i][right.center[1] + 1] = true;
                }
                if (!grid[wallX][right.center[1] - 1]) {
                    world[right.center[0] - i][right.center[1] - 1] = WALL;
                    grid[right.center[0] - i][right.center[1] - 1] = true;
                }
            }

            boolean breakWall = right.x > low.center[0];
            boolean breakCeiling = right.center[1] > low.y + low.ROOM_HEIGHT;
            if (breakWall) {
                world[right.x][right.center[1]] = FLOOR;
                grid[right.x][right.center[1]] = true;
            }
            if (breakCeiling) {
                world[low.center[0]][low.y + low.ROOM_HEIGHT] = FLOOR;
                grid[low.center[0]][low.y + low.ROOM_HEIGHT] = true;
            }
            if (!grid[low.center[0] - 1][right.center[1] + 1]) {
                world[low.center[0] - 1][right.center[1] + 1] = WALL;
                grid[low.center[0] - 1][right.center[1] + 1] = true;
            }
        } else {
            for (int i = 1; i <= xMove; i++) {
                int wallX = left.center[0] + i;
                world[wallX][left.center[1]] = FLOOR;
                grid[wallX][left.center[1]] = true;
                if (!grid[wallX][left.center[1] + 1]) {
                    world[wallX][left.center[1] + 1] = WALL;
                    grid[wallX][left.center[1] + 1] = true;
                }
                if (!grid[wallX][left.center[1] - 1]) {
                    world[wallX][left.center[1] - 1] = WALL;
                    grid[wallX][left.center[1] - 1] = true;
                }
            }
            boolean breakWall = left.x > low.center[0];
            boolean breakCeiling = left.center[1] > low.y + low.ROOM_HEIGHT;
            if (breakWall) {
                world[left.x][left.center[1]] = FLOOR;
                grid[left.x][left.center[1]] = true;
            }
            if (breakCeiling) {
                world[low.center[0]][low.y + low.ROOM_HEIGHT] = FLOOR;
                grid[low.center[0]][low.y + low.ROOM_HEIGHT] = true;
            }
            if (!grid[low.center[0] + 1][left.center[1] + 1]) {
                world[low.center[0] + 1][left.center[1] + 1] = WALL;
                grid[low.center[0] + 1][left.center[1] + 1] = true;
            }
        }
    }
}
