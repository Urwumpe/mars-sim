package com.mars_sim.fxgl.model.operations;

import com.mars_sim.fxgl.model.util.Loader;

public class MineOperation extends Operation {

    public static final String DEFAULT = "operations/mine.png";
    private int points_per_second;

    public MineOperation(int currentLevel) {
        super(Loader.loadImage(DEFAULT), currentLevel);
        doOperation();
    }

    @Override
    public void init() {
      //
    }

    @Override
    public int getPrice() {
        return (super.getLevel() + 1) * 16000;
    }

    @Override
    public void doOperation() {
        points_per_second = super.getLevel() * 100;
    }

    @Override
    public String operationName() {
        return "MINE";
    }

    public int getPointsPerSecond() {
        return points_per_second;
    }
}
