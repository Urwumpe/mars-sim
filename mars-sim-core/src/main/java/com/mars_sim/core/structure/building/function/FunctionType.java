/*
 * Mars Simulation Project
 * FunctionType.java
 * @date 2023-09-19
 * @author stpa				
 */
package com.mars_sim.core.structure.building.function;

import com.mars_sim.core.robot.RobotType;
import com.mars_sim.tools.Msg;

public enum FunctionType {

    ADMINISTRATION              (Msg.getString("FunctionType.administration")), //$NON-NLS=1$
	ALGAE_FARMING				(Msg.getString("FunctionType.algaeFarming")), //$NON-NLS-1$
    ASTRONOMICAL_OBSERVATION	(Msg.getString("FunctionType.astronomicalObservations")), //$NON-NLS-1$
	BUILDING_CONNECTION			(Msg.getString("FunctionType.buildingConnection")), //$NON-NLS-1$
	COMMUNICATION				(Msg.getString("FunctionType.communication")), //$NON-NLS-1$
	COMPUTATION					(Msg.getString("FunctionType.computation")), //$NON-NLS-1$
	COOKING						(Msg.getString("FunctionType.cooking")), //$NON-NLS-1$
	DINING						(Msg.getString("FunctionType.dining")), //$NON-NLS-1$
	EARTH_RETURN				(Msg.getString("FunctionType.earthReturn")), //$NON-NLS-1$
	EVA							(Msg.getString("FunctionType.eva")), //$NON-NLS-1$
	EXERCISE					(Msg.getString("FunctionType.exercise")), //$NON-NLS-1$
	FARMING						(Msg.getString("FunctionType.farming")), //$NON-NLS-1$
	FISHERY						(Msg.getString("FunctionType.fishery")), //$NON-NLS-1$
	FOOD_PRODUCTION  			(Msg.getString("FunctionType.foodProduction")), //$NON-NLS-1$	
	VEHICLE_MAINTENANCE			(Msg.getString("FunctionType.vehicleMaintenance")), //$NON-NLS-1$
	LIFE_SUPPORT				(Msg.getString("FunctionType.lifeSupport")), //$NON-NLS-1$
	LIVING_ACCOMMODATIONS		(Msg.getString("FunctionType.livingAccommodations")), //$NON-NLS-1$
	MANAGEMENT					(Msg.getString("FunctionType.management")), //$NON-NLS-1$
	MANUFACTURE					(Msg.getString("FunctionType.manufacture")), //$NON-NLS-1$
	MEDICAL_CARE				(Msg.getString("FunctionType.medicalCare")), //$NON-NLS-1$
	POWER_GENERATION			(Msg.getString("FunctionType.powerGeneration")), //$NON-NLS-1$
	POWER_STORAGE				(Msg.getString("FunctionType.powerStorage")), //$NON-NLS-1$
	PREPARING_DESSERT			(Msg.getString("FunctionType.preparingDessert")),  //$NON-NLS-1$
	RECREATION					(Msg.getString("FunctionType.recreation")), //$NON-NLS-1$
	RESEARCH					(Msg.getString("FunctionType.research")), //$NON-NLS-1$
	RESOURCE_PROCESSING			(Msg.getString("FunctionType.resourceProcessing")), //$NON-NLS-1$
	ROBOTIC_STATION				(Msg.getString("FunctionType.roboticStation")), //$NON-NLS-1$
	STORAGE						(Msg.getString("FunctionType.storage")),  //$NON-NLS-1$
	THERMAL_GENERATION			(Msg.getString("FunctionType.thermalGeneration")), //$NON-NLS-1$
	WASTE_PROCESSING			(Msg.getString("FunctionType.wasteProcessing")), //$NON-NLS-1$
	FIELD_STUDY					(Msg.getString("FunctionType.fieldStudy")), //$NON-NLS-1$
	UNKNOWN						(Msg.getString("FunctionType.unknown")) //$NON-NLS-1$
	;

	private String name;


	/** hidden constructor. */
	private FunctionType(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
	
	/**
	 * Gets the default Function for a Robot Type.
	 * 
	 * @return FunctionType
	 */
	public static FunctionType getDefaultFunction(RobotType type) {
		switch (type) {
		case CHEFBOT:
			return FunctionType.COOKING;
		
		case CONSTRUCTIONBOT:
			return FunctionType.MANUFACTURE;
			
		case DELIVERYBOT:
			return FunctionType.VEHICLE_MAINTENANCE;
			
		case GARDENBOT:
			return FunctionType.FARMING;
			
		case MAKERBOT:
			return FunctionType.MANUFACTURE;
			
		case MEDICBOT:
			return FunctionType.MEDICAL_CARE;
			
		case REPAIRBOT:
			return FunctionType.LIFE_SUPPORT;
			
		default:
			return FunctionType.ROBOTIC_STATION;
		}
	}
}
