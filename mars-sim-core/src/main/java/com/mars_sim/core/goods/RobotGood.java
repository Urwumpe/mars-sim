/*
 * Mars Simulation Project
 * RobotGood.java
 * @date 2022-06-26
 * @author Barry Evans
 */
package com.mars_sim.core.goods;

import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.robot.RobotType;
import com.mars_sim.core.structure.Settlement;

/**
 * This class represents how a Robot can be traded.
 */
class RobotGood extends Good {
	
	private static final long serialVersionUID = 1L;
	
    private static final double INITIAL_ROBOT_DEMAND = 10;
    private static final double INITIAL_ROBOT_SUPPLY = 10;

	private static final int ROBOT_VALUE = 200;

    private RobotType robotType;

    public RobotGood(RobotType type) {
        super(type.getName(), RobotType.getResourceID(type));
        this.robotType = type;
    }

    @Override
    public GoodCategory getCategory() {
        return GoodCategory.ROBOT;
    }

    @Override
    public double getMassPerItem() {
        return Robot.EMPTY_MASS;
    }

    @Override
    public GoodType getGoodType() {
        // TODO Must be a better way 
        switch(robotType) {
            case CHEFBOT: return GoodType.CHEFBOT;
            case CONSTRUCTIONBOT: return GoodType.CONSTRUCTIONBOT;
            case DELIVERYBOT: return GoodType.DELIVERYBOT;
            case GARDENBOT: return GoodType.GARDENBOT;
            case MAKERBOT: return GoodType.MAKERBOT;
            case MEDICBOT: return GoodType.MEDICBOT;
            case REPAIRBOT: return GoodType.REPAIRBOT;
        }

        throw new IllegalStateException("Cannot mapt robot type " + robotType + " to GoodType");
    }

    @Override
    protected double computeCostModifier() {
        return ROBOT_VALUE;
    }

    @Override
    public double getNumberForSettlement(Settlement settlement) {
		// Get number of robots.
		return (int) settlement.getAllAssociatedRobots().stream()
                        .filter( r -> r.getRobotType() == robotType)
                        .count();	
    }

    @Override
    double getPrice(Settlement settlement, double value) {
        double mass = Robot.EMPTY_MASS;
        double quantity = settlement.getInitialNumOfRobots() ;
        double factor = Math.log(mass/50.0 + 1) / (5 + Math.log(quantity + 1));
        // Need to increase the value for robots
        return getCostOutput() * (1 + 2 * factor * Math.log(value + 1));
    }

    @Override
    double getDefaultDemandValue() {
        return INITIAL_ROBOT_DEMAND;
    }

    @Override
    double getDefaultSupplyValue() {
        return INITIAL_ROBOT_SUPPLY;
    }

    @Override
    void refreshSupplyDemandValue(GoodsManager owner) {
        // There is no logic to calculate Supply & Demand for Robots currently
    }
}
