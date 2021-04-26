package com.codingame.game.gota;

public class Cell {
    public int x;
    public int y;
    public boolean wall;
    public Unit unit;

    Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.wall = false;
        this.unit = null;
    }

    Unit setUnit(int owner) {
        this.unit = new Unit(this, owner);
        return this.unit;
    }

    public void setWall() {
        this.wall = true;
    }

    @Override
    public String toString() {
        return Character.toString((char)(97 + x)) + -~y;
    }
}
