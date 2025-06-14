package core;

import tileengine.TETile;
import tileengine.TERenderer;
import tileengine.Tileset;
import edu.princeton.cs.algs4.StdDraw;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.*;

public class Main {
    public static void main(String[] args) throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        StdDraw.setCanvasSize(World.WIDTH * 16, World.HEIGHT * 16);
        StdDraw.setXscale(0, World.WIDTH);
        StdDraw.setYscale(0, World.HEIGHT);
        StdDraw.clear(Color.BLACK);
        StdDraw.enableDoubleBuffering();
        displayMainMenu();
    }
    public static void displayMainMenu() throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        boolean gameRunning = true;
        while (gameRunning) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 30));
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.text(World.WIDTH / 2.0, World.HEIGHT * 0.6, "New Game (N)");
            StdDraw.text(World.WIDTH / 2.0, World.HEIGHT * 0.5, "Load Game (L)");
            StdDraw.text(World.WIDTH / 2.0, World.HEIGHT * 0.4, "Quit (Q)");
            StdDraw.show();
            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                switch (key) {
                    case 'n' -> startNewGame();
                    case 'l' -> loadGame();
                    case 'q' -> {
                        gameRunning = false;
                        System.exit(0);
                    }
                    default -> System.out.println("Invalid selection! Please enter N, L, or Q.");
                }
            }
        }
    }
    public static void startNewGame() throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        StringBuilder seed = new StringBuilder();
        boolean gameStarted = false;
        while (!gameStarted) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 30));
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.text(World.WIDTH / 2.0, World.HEIGHT * 0.8, "Enter Seed:");
            StdDraw.text(World.WIDTH / 2.0, World.HEIGHT * 0.5, seed.toString());
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 20));
            StdDraw.text(World.WIDTH / 2.0, World.HEIGHT * 0.3, "Press S to start");
            StdDraw.show();
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                if ((key == 'S' || key == 's') && seed.length() > 0) {
                    gameStarted = true;
                } else if (Character.isDigit(key)) {
                    seed.append(key);
                }
            }
        }
        long seedValue = Long.parseLong(seed.toString());
        World world = new World();
        world.generate(seedValue);
        TERenderer ter = new TERenderer();
        ter.initialize(World.WIDTH, World.HEIGHT);
        boolean inGame = true;
        boolean commandMode = false;
        while (inGame) {
            renderWithLineOfSight(ter, world);
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                if (commandMode) {
                    if (key == 'q' || key == 'Q') {
                        saveGame(world);
                        System.exit(0);
                    } else {
                        commandMode = false;
                    }
                } else {
                    if (key == ':') {
                        commandMode = true;
                    } else if (key == 'l' || key == 'L') {
                        world.toggleLineOfSight();
                    } else {
                        world.moveAvatar(key);
                    }
                }
            }
        }
    }
    private static void renderWithLineOfSight(TERenderer ter, World world) {
        TETile[][] displayWorld = new TETile[World.WIDTH][World.HEIGHT];
        for (int x = 0; x < World.WIDTH; x++) {
            for (int y = 0; y < World.HEIGHT; y++) {
                if (world.isTileVisible(x, y)) {
                    displayWorld[x][y] = world.world[x][y];
                } else {
                    displayWorld[x][y] = Tileset.NOTHING;
                }
            }
        }
        ter.renderFrame(displayWorld);
    }
    public static void saveGame(World world) {
        File f = new File("./save_data.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(f))) {
            writer.write(world.getSeed() + "\n");
            writer.write(world.getAvatarPosition().line + " " + world.getAvatarPosition().column + "\n");
            for (int y = 0; y < World.HEIGHT; y++) {
                for (int x = 0; x < World.WIDTH; x++) {
                    writer.write(world.toCharacter(world.world[x][y]));
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    public static void loadGame() throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        File f = new File("./save_data.txt");
        if (f.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                long seed = Long.parseLong(reader.readLine());
                String[] avatarPos = reader.readLine().split(" ");
                int avatarX = Integer.parseInt(avatarPos[0]);
                int avatarY = Integer.parseInt(avatarPos[1]);
                World world = new World();
                world.generate(seed);
                world.setAvatarPosition(new World.Position(avatarX, avatarY));
                for (int y = 0; y < World.HEIGHT; y++) {
                    String line = reader.readLine();
                    for (int x = 0; x < World.WIDTH; x++) {
                        world.world[x][y] = world.fromCharacter(line.charAt(x));
                    }
                }
                TERenderer ter = new TERenderer();
                ter.initialize(World.WIDTH, World.HEIGHT);

                boolean inGame = true;
                boolean commandMode = false;
                while (inGame) {
                    ter.renderFrame(world.world);
                    if (StdDraw.hasNextKeyTyped()) {
                        char key = StdDraw.nextKeyTyped();
                        if (commandMode) {
                            if (key == 'q' || key == 'Q') {
                                saveGame(world);
                                System.exit(0);
                            } else {
                                commandMode = false;
                            }
                        } else {
                            if (key == ':') {
                                commandMode = true;
                            } else if (key == 'l' || key == 'L') {
                                world.toggleLineOfSight();
                            } else {
                                world.moveAvatar(key);
                            }
                        }
                    }
                }
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.out.println("No saved game to load.");
            displayMainMenu();
        }
    }
}
