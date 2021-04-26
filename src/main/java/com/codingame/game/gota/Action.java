package com.codingame.game.gota;

import java.util.ArrayList;
import java.util.List;

public class Action {
    public Unit unit;
    public Cell target;
    public List<Cell> arrows;

    Action(Unit unit, Cell target, ArrayList<Cell> arrows) {
        this.unit = unit;
        this.target = target;
        this.arrows = arrows;
    }

    @Override
    public String toString() {
        return unit.toString() + target.toString();
    }
}
