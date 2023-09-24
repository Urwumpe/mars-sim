/*
 * Mars Simulation Project
 * FieldStudyMeta.java
 * @date 2022-07-14
 * @author Barry Evans
 */
package org.mars_sim.msp.core.person.ai.mission.meta;

import java.util.Set;

import org.mars_sim.msp.core.data.RatingScore;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.mission.FieldStudyMission;
import org.mars_sim.msp.core.person.ai.mission.MissionType;
import org.mars_sim.msp.core.person.ai.mission.RoverMission;
import org.mars_sim.msp.core.person.ai.role.RoleType;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.vehicle.Rover;

public class FieldStudyMeta extends AbstractMetaMission {

	private static final Set<RoleType> PREFERRED_ROLES = Set.of(RoleType.CHIEF_OF_SCIENCE,
							RoleType.MISSION_SPECIALIST, RoleType.CHIEF_OF_MISSION_PLANNING,
							RoleType.SCIENCE_SPECIALIST, RoleType.COMMANDER,
							RoleType.SUB_COMMANDER);
	private static final String STUDY_BASE = "study";
	private static final double WEIGHT = 10D;
	private ScienceType science;

	public FieldStudyMeta(MissionType type, Set<JobType> preferredLeaderJob,
			ScienceType science) {
		super(type, preferredLeaderJob);
		this.science = science;
	}

	@Override
	public RatingScore getProbability(Person person) {

		RoleType roleType = person.getRole().getType();
		JobType jobType = person.getMind().getJob();

		if ((FieldStudyMission.determineStudy(science, person) == null)
			|| !person.isInSettlement()
			|| (!getPreferredLeaderJob().contains(jobType)
					&& !PREFERRED_ROLES.contains(roleType))) {
			return RatingScore.ZERO_RATING;
		}

		Settlement settlement = person.getSettlement();

		RatingScore missionProbability = new RatingScore(STUDY_BASE, 1D);
			
		// Get available rover.
		Rover rover = RoverMission.getVehicleWithGreatestRange(settlement, false);
		if (rover != null) {
			double newBase = 0;

			// Add probability for researcher's primary study (if any).
			ScientificStudy primaryStudy = person.getStudy();
			if ((primaryStudy != null) && ScientificStudy.RESEARCH_PHASE.equals(primaryStudy.getPhase())
					&& !primaryStudy.isPrimaryResearchCompleted()
					&& (science == primaryStudy.getScience())) {
				newBase += WEIGHT;
			}

			// Add probability for each study researcher is collaborating on.
			for(ScientificStudy collabStudy : person.getCollabStudies()) {
				if (ScientificStudy.RESEARCH_PHASE.equals(collabStudy.getPhase())
						&& !collabStudy.isCollaborativeResearchCompleted(person)
						&& (science == collabStudy.getContribution(person))) {
					newBase += WEIGHT/2D;
				}
			}

			missionProbability.addBase(STUDY_BASE, newBase);
		}

		missionProbability.addModifier(SETTLEMENT_POPULATION,
						getSettlementPopModifier(settlement, 4));

		// Crowding modifier
		int crowding = settlement.getIndoorPeopleCount() - settlement.getPopulationCapacity();
		if (crowding > 0) missionProbability.addModifier(OVER_CROWDING, crowding + 1);

		// Job modifier.
		missionProbability.addModifier(LEADER, getLeaderSuitability(person));
		missionProbability.addModifier(GOODS, (settlement.getGoodsManager().getTourismFactor()
				+ settlement.getGoodsManager().getResearchFactor())/1.5);

		// if introvert, score  0 to  50 --> -2 to 0
		// if extrovert, score 50 to 100 -->  0 to 2
		// Reduce probability if introvert
		int extrovert = person.getExtrovertmodifier();
		missionProbability.addModifier(PERSON_EXTROVERT, (1 + extrovert/2.0));

		missionProbability.applyRange(0, LIMIT);

	    return missionProbability;
	}
}