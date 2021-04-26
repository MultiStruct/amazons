package com.codingame.game.gota;

import com.codingame.game.Player;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.Curve;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Rectangle;
import com.codingame.gameengine.module.toggle.ToggleModule;

import java.util.ArrayList;
import java.util.List;

public class Viewer {
    MultiplayerGameManager<Player> gameManager;
    GraphicEntityModule graphics;
    Rectangle[][] rectangles;
    List<UnitUI> units;
    Rectangle[] lastActions;
    public PlayerUI[] playerUIS;

    int HEIGHT;
    int WIDTH;
    int VIEWER_WIDTH;
    int VIEWER_HEIGHT;
    int RECTANGLE_SIZE;
    int CIRCLE_RADIUS;
    int GAP;
    final int[] BOARDCOLORS = new int[]{0xF0D9B5, 0x946f51};
    final int BACKGROUNDCOLOR = 0x262421;
    final int[] HIGHLIGHTCOLOR = new int[]{0x7e9ec2, 0x7e9ec2, 0xFF0000};
    final int WALLCOLOR = 0x191919;
    final double MOVE_TIME = 350;
    final double WALL_TIME = 500;

    public void reset(Board board) {
        gameManager.setFrameDuration(1800);
        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                rectangles[y][x].setFillColor(BOARDCOLORS[(x ^ y)  % 2]);
                graphics.commitEntityState(0.4, rectangles[y][x]);
            }
        }

        for (int i = 0; i < 3; ++i) {
            lastActions[i].setX(-RECTANGLE_SIZE, Curve.IMMEDIATE);
        }

        for (int i = 0; i < 8; ++i) {
            graphics.commitEntityState(0.4, units.get(i).sprite);
            units.get(i).unit = board.units.get(i);
            int x = board.units.get(i).getX();
            int y = board.units.get(i).getY();
            units.get(i).sprite.setX(rectangles[y][x].getX() + GAP).setY(rectangles[y][x].getY() + GAP);
            graphics.commitEntityState(0.8, units.get(i).sprite);
        }

        for (int i = 0; i < 2; ++i) {
            playerUIS[i].msg.setText("");
            playerUIS[i].action.setText("");
        }

        playerUIS[0].piece.setImage("b.png");
        playerUIS[1].piece.setImage("w.png");
    }

    public Viewer(GraphicEntityModule graphics, Board board, MultiplayerGameManager<Player> gameManager, ToggleModule toggleModule) {
        this.graphics = graphics;
        this.gameManager = gameManager;

        VIEWER_WIDTH = this.graphics.getWorld().getWidth();
        VIEWER_HEIGHT = this.graphics.getWorld().getHeight();
        HEIGHT = board.HEIGHT;
        WIDTH = board.WIDTH;

        playerUIS = new PlayerUI[2];
        rectangles = new Rectangle[HEIGHT][WIDTH];
        lastActions = new Rectangle[3];
        units = new ArrayList<>();

        this.graphics.createRectangle().setWidth(VIEWER_WIDTH).setHeight(VIEWER_HEIGHT).setFillColor(BACKGROUNDCOLOR);

        RECTANGLE_SIZE = VIEWER_HEIGHT / -~HEIGHT;
        int START_X = VIEWER_WIDTH / 2 - RECTANGLE_SIZE * WIDTH / 2;
        int FONT_SIZE = RECTANGLE_SIZE / 3;

        for (int y = 0; y < HEIGHT; ++y) {
            int yG = HEIGHT - y - 1;
            for (int x = 0; x < WIDTH; ++x) {
                int xG = x;
                rectangles[y][x] = graphics.createRectangle().setFillColor(BOARDCOLORS[(x + y + 1)  & 1]).setWidth(RECTANGLE_SIZE).setHeight(RECTANGLE_SIZE).setX(START_X + xG * RECTANGLE_SIZE).setY((int)(RECTANGLE_SIZE / 2) + yG * RECTANGLE_SIZE - FONT_SIZE / 2);
                if(x == 0) {
                    int length = -~y < 10 ? 1 : (int)(Math.log10(x) + 1);
                    graphics.createText(Integer.toString(-~y)).setX(rectangles[y][x].getX() - (int)(RECTANGLE_SIZE / 1.5) + (int)(FONT_SIZE / length * .5)).setY(rectangles[y][x].getY() + FONT_SIZE).setFontFamily("Verdana").setFontSize(FONT_SIZE).setFillColor(0xFEFEFE);
                }

                if(y == 0) {
                    graphics.createText(Character.toString((char) (97+x))).setX(rectangles[y][x].getX() + (int)(FONT_SIZE * (1.25))).setY(rectangles[y][x].getY() + RECTANGLE_SIZE + FONT_SIZE / 4).setFontFamily("Verdana").setFontSize(FONT_SIZE).setFillColor(0xFEFEFE);
                }
            }
        }

        CIRCLE_RADIUS = (int)(RECTANGLE_SIZE * .42);
        GAP = (RECTANGLE_SIZE - CIRCLE_RADIUS * 2) / 2;

        for (int i = 0; i < 3; ++i) {
            lastActions[i] = graphics.createRectangle().setFillColor(HIGHLIGHTCOLOR[i]).setWidth(RECTANGLE_SIZE).setHeight(RECTANGLE_SIZE).setX(-RECTANGLE_SIZE).setY(-RECTANGLE_SIZE).setFillAlpha(i < 2 ? 0.75 : 0.45);
            toggleModule.displayOnToggleState(lastActions[i], "debugToggle", true);
        }

        for (Unit unit : board.units) {
            units.add(new UnitUI(unit, this));
        }

        for (int i = 0; i < 2; ++i) {
            playerUIS[i] = new PlayerUI(this, gameManager.getPlayer(i));
        }
    }

    public void applyAction(Unit unit, Cell target, Cell arrow) {
        Rectangle rect = rectangles[arrow.y][arrow.x];
        double distance = Math.pow(Math.max(Math.abs(unit.getX() - target.x),Math.abs(unit.getY() - target.y)), 0.45);
        double frameDuration = distance * MOVE_TIME + WALL_TIME;
        gameManager.setFrameDuration((int)frameDuration);
        double commitTime = 1.0 - (WALL_TIME / frameDuration);
        commitTime = Double.parseDouble(String.format("%.2f", commitTime));

        for (UnitUI u : units) {
            if (u.unit != unit) continue;
            if (unit.getX() != arrow.x || unit.getY() != arrow.y)
                lastActions[0].setX(rectangles[unit.getY()][unit.getX()].getX(), Curve.IMMEDIATE).setY(rectangles[unit.getY()][unit.getX()].getY(), Curve.IMMEDIATE);
            else
                lastActions[0].setX(-RECTANGLE_SIZE, Curve.IMMEDIATE);

            int x = target.x;
            int y = target.y;
            lastActions[1].setX(rectangles[y][x].getX(), Curve.IMMEDIATE).setY(rectangles[y][x].getY(), Curve.IMMEDIATE);

            u.sprite.setX(rectangles[y][x].getX() + GAP).setY(rectangles[y][x].getY() + GAP);
            graphics.commitEntityState(commitTime, u.sprite, rect);
            break;
        }

        lastActions[2].setX(rectangles[arrow.y][arrow.x].getX(), Curve.IMMEDIATE).setY(rectangles[arrow.y][arrow.x].getY(), Curve.IMMEDIATE);
        graphics.commitEntityState(commitTime, lastActions[2]);


        rect.setFillColor(WALLCOLOR);
    }
}
