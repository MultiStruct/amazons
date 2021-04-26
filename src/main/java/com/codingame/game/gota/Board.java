package com.codingame.game.gota;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board {
    long seed;
    int HEIGHT;
    int WIDTH;
    public Cell[][] cells;
    List<Unit> units;

    public int getHEIGHT() {
        return HEIGHT;
    }

    public int getWIDTH() {
        return WIDTH;
    }

    public Board(int size, long seed) {
        this.seed = seed;
        HEIGHT= size;
        WIDTH = size;
        cells = new Cell[HEIGHT][WIDTH];
        units = new ArrayList<>();

        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0;  x < WIDTH; ++x) {
                cells[y][x] = new Cell(x, y);
            }
        }

        setStartingPositions();
    }

    // Currently creates a board with pieces on each 4x4, it allows for 1 piece to be next to each other as long as it is in a different sub board
    void setStartingPositions() {
        int [][] blacklistedPositions = new int[][] { {0, 0}, {0, WIDTH - 1}, {HEIGHT - 1, 0}, {HEIGHT - 1, WIDTH - 1}};
        int [][] zones = new int[][] { {0, 0}, {0, WIDTH / 2}, {HEIGHT / 2, 0}, {HEIGHT / 2, WIDTH / 2} };
        Random random = new Random(seed);

        while (units.size() < 8) {
            int y = random.nextInt(HEIGHT / 2);
            int x = random.nextInt(WIDTH / 2);

            y += zones[units.size() >> 1][0];
            x += zones[units.size() >> 1][1];

            if (!isEmpty(x, y)) continue;
            if (hasUnitAround(x, y)) continue;


            boolean blacklisted = false;
            for (int [] position : blacklistedPositions) {
                if (position[0] != x || position[1] != y) continue;

                blacklisted = true;
                break;
            }

            if (blacklisted) continue;

            units.add(cells[y][x].setUnit(0));
            units.add(cells[HEIGHT - y - 1][WIDTH - x - 1].setUnit(1));
        }
    }

    // Returns if player lost.
    public boolean hasPlayerLost(int player) {
        for(Unit unit : units) {
            if (unit.owner != player) continue;
            for (Direction dir : Direction.values()) {
                int x = unit.getX() + dir.x;
                int y = unit.getY() + dir.y;
                if (!isEmpty(x, y)) continue;

                return false;
            }
        }
        return true;
    }

    public ArrayList<Action> getLegalActions(int player) {
        ArrayList<Action> actions = new ArrayList<>();

        for(Unit unit : units) {
            if(unit.owner != player) continue;
            Cell cell = unit.cell;
            cell.unit = null;

            for (Direction dir : Direction.values()) {
                int x = unit.getX();
                int y = unit.getY();

                while (true) {
                    x += dir.x;
                    y += dir.y;

                    if (!isEmpty(x, y)) break;

                    Action action = new Action(unit, cells[y][x], getLegalArrows(cells[y][x]));

                    actions.add(action);
                }
            }

            cell.unit = unit;
        }

        return actions;
    }

    ArrayList<Cell> getLegalArrows(Cell cell) {
        ArrayList<Cell> arrows = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            int x = cell.x;
            int y = cell.y;

            while (true) {
                x += dir.x;
                y += dir.y;
                if (!isEmpty(x, y)) break;

                arrows.add(cells[y][x]);
            }
        }

        return arrows;
    }

    public void applyAction(Unit unit, Cell target, Cell arrow) {
        unit.moveTo(target);
        arrow.setWall();
    }

    boolean isInside(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    boolean isEmpty(int x, int y) {
        if (isInside(x, y))
            return cells[y][x].unit == null && !cells[y][x].wall;
        return false;
    }

    boolean hasUnitAround(int _x, int _y) {
        for (Direction dir : Direction.values()) {
            int x = _x + dir.x;
            int y = _y + dir.y;

            if (!isInside(x, y)) continue;
            if (isEmpty(x, y)) continue;

            return true;
        }
        return false;
    }
}
