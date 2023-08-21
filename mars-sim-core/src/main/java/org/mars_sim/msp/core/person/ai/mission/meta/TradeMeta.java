/*
 * Mars Simulation Project
 * TradeMeta.java
 * @date 2021-08-28
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission.meta;

import java.util.Set;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.data.Rating;
import org.mars_sim.msp.core.goods.Deal;
import org.mars_sim.msp.core.goods.GoodsManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionType;
import org.mars_sim.msp.core.person.ai.mission.RoverMission;
import org.mars_sim.msp.core.person.ai.mission.Trade;
import org.mars_sim.msp.core.person.ai.mission.MissionUtil;
import org.mars_sim.msp.core.person.ai.role.RoleType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * A meta mission for the Trade mission.
 */
public class TradeMeta extends AbstractMetaMission {

	/** default logger. */
//	private static SimLogger logger = SimLogger.getLogger(TradeMeta.class.getName());


	TradeMeta() {
		super(MissionType.TRADE, 
				Set.of(JobType.POLITICIAN, JobType.TRADER, JobType.REPORTER));
	}

	@Override
	public Mission constructInstance(Person person, boolean needsReview) {
		return new Trade(person, needsReview);
	}

	@Override
	public Rating getProbability(Person person) {

		Rating missionProbability = Rating.ZERO_RATING;

		Settlement settlement = person.getAssociatedSettlement();
		
		if (settlement.isFirstSol())
			return missionProbability;
		
		// Check if person is in a settlement.
		if (settlement != null) {
	
			RoleType roleType = person.getRole().getType();
			
			if (RoleType.CHIEF_OF_SUPPLY_N_RESOURCES == roleType
					|| RoleType.RESOURCE_SPECIALIST == roleType
		 			|| RoleType.MISSION_SPECIALIST == roleType
		 			|| RoleType.CHIEF_OF_MISSION_PLANNING == roleType	
					|| RoleType.SUB_COMMANDER == roleType
					|| RoleType.COMMANDER == roleType
					) {
			
					// Note: checkMission() gives rise to a NULLPOINTEREXCEPTION that points to
					// Inventory
					// It happens only when this sim is a loaded saved sim.
					missionProbability = getSettlementProbability(settlement);
			}
			
			if (missionProbability.getScore() <= 0)
				return missionProbability;

			// if introvert, score  0 to  50 --> -2 to 0
			// if extrovert, score 50 to 100 -->  0 to 2
			// Reduce probability if introvert
			int extrovert = person.getExtrovertmodifier();
			missionProbability.addModifier(PERSON_EXTROVERT, (1 + extrovert/2.0));
			missionProbability.applyRange(0, LIMIT);
		}

        
		return missionProbability;
	}

	private Rating getSettlementProbability(Settlement settlement) {
		
		// Check for the best trade settlement within range.			
		Rover rover = RoverMission.getVehicleWithGreatestRange(settlement, false);
		if (rover == null) {
			return Rating.ZERO_RATING;
		}
		GoodsManager gManager = settlement.getGoodsManager();

		Deal deal = gManager.getBestDeal(MissionType.TRADE, rover);
		if (deal == null) {
			return Rating.ZERO_RATING;
		}	

		int numEmbarked = MissionUtil.numEmbarkingMissions(settlement);
		int numThisMission = Simulation.instance().getMissionManager().numParticularMissions(MissionType.TRADE, settlement);

   		// Check for # of embarking missions.
		if (Math.max(1, settlement.getNumCitizens() / 4.0) < numEmbarked + numThisMission) {
			return Rating.ZERO_RATING;
		}			
		else if (numThisMission > 1)
			return Rating.ZERO_RATING;	

		double tradeProfit = deal.getProfit();

		// Trade value modifier.
		Rating missionProbability = new Rating(tradeProfit / 1000D * gManager.getTradeFactor());
		missionProbability.applyRange(0, Trade.MAX_STARTING_PROBABILITY);


		int f1 = 2*numEmbarked + 1;
		int f2 = 2*numThisMission + 1;
		
		missionProbability.addModifier("Mission Ratio", settlement.getNumCitizens() / f1 / f2 / 2D);
		
		// Crowding modifier.
		int crowding = settlement.getIndoorPeopleCount() - settlement.getPopulationCapacity();
		if (crowding > 0) {
			missionProbability.addModifier("Over Crowding", (crowding + 1));
		}

		return missionProbability;
	}	
}
