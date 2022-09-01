/*
 * Mars Simulation Project
 * Teach.java
 * @date 2022-09-01
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.social.RelationshipUtil;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.person.ai.task.utils.Worker;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingCategory;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.LifeSupport;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * This is a task for teaching a student a task.
 */
public class Teach extends Task implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static SimLogger logger = SimLogger.getLogger(Teach.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.teach"); //$NON-NLS-1$
	
	/** Task phases. */
	private static final TaskPhase TEACHING = new TaskPhase(Msg.getString("Task.phase.teaching")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = -.1D;

	/**
	 * The improvement in relationship opinion of the teacher from the student per
	 * millisol.
	 */
	private static final double BASE_RELATIONSHIP_MODIFIER = .2D;

	// Data members
	private Person student;
	private Task teachingTask;

	/**
	 * Constructor.
	 * 
	 * @param unit the unit performing the task.
	 */
	public Teach(Worker unit) {
		super(NAME, unit, false, false, STRESS_MODIFIER, null, 10D);
		
		if (unit.getUnitType() == UnitType.PERSON)
			person = (Person) unit;
		else
			robot = (Robot) unit;
		
		// Initialize phase
		addPhase(TEACHING);
		setPhase(TEACHING);
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (TEACHING.equals(getPhase())) {
			return teachingPhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Performs the teaching phase.
	 * 
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double teachingPhase(double time) {

		if (teachingTask == null) {
			
			// Assume the student is a person.
			Collection<Person> candidates = null;
			List<Person> students = new ArrayList<>();
			
			if (worker.getUnitType() == UnitType.PERSON)
				candidates = getBestStudents(person);
			else
				candidates = getBestStudents(robot);
			
			Iterator<Person> i = candidates.iterator();
			while (i.hasNext()) {
				Person candidate = i.next();
				if (worker.getUnitType() == UnitType.PERSON)
					logger.log(person, Level.FINE, 4_000, "Connecting with student " + candidate.getName() + ".");
				else
					logger.log(robot, Level.FINE, 4_000, "Connecting with student " + candidate.getName() + ".");
				
				students.add(candidate);
			}
			
			if (students.size() > 0) {
				Object[] array = students.toArray();
				// Randomly get a person student.
				int rand = RandomUtil.getRandomInt(students.size() - 1);
				student = (Person) array[rand];
				// Gets the task the studen is doing
				teachingTask = student.getMind().getTaskManager().getTask();
				
				if (worker.getUnitType() == UnitType.PERSON) {
//					teachingTask.setTeacher(person);
					logger.log(person, Level.INFO, 10_000, "Teaching " + student.getName() 
					+ " on " + teachingTask.getName(false) + ".");
				}
				else {
//					teachingTask.setTeacher(robot);
					logger.log(robot, Level.INFO, 10_000, "Teaching " + student.getName() 
					+ " on " + teachingTask.getName(false) + ".");
				}
				
				setDescription(
						Msg.getString("Task.description.teach.detail", 
								teachingTask.getName(false), student.getName())); // $NON-NLS-1$

				boolean walkToBuilding = false;
				
				boolean isInSettlement = false;
				if (worker.getUnitType() == UnitType.PERSON) {
					isInSettlement = person.isInSettlement();
				}
				else {
					isInSettlement = robot.isInSettlement();
				}
				
				if (isInSettlement) {

					// If in settlement, move teacher to the building where student is in.
					Building studentBuilding = BuildingManager.getBuilding(student);

					if (studentBuilding != null && 
							studentBuilding.getCategory() != BuildingCategory.EVA_AIRLOCK) {
						// Walk to random location in student's building.
						walkToRandomLocInBuilding(BuildingManager.getBuilding(student), false);
				
						walkToBuilding = true;
					}
				}

				if (!walkToBuilding) {
					if (person.isInVehicle()) {
						// If person is in rover, walk to passenger activity spot.
						if (person.getVehicle() instanceof Rover) {
							walkToPassengerActivitySpotInRover((Rover) person.getVehicle(), false);
						}
					} else {
						// Walk to random location.
						walkToRandomLocation(true);
					}
				}
			} else {
				endTask();
			}
		}

		// Check if task is finished.
		if (teachingTask != null && teachingTask.isDone())
			endTask();
		
    	if (getTimeCompleted() > getDuration())
        	endTask();
    	
    	if (worker.getUnitType() == UnitType.PERSON) {
    		if (!person.isBarelyFit())
    			endTask();
    		// Add relationship modifier for opinion of teacher from the student.
    		addRelationshipModifier(time);
    	}

        // Add experience points
        addExperience(time);
        
		return 0D;
	}

	/**
	 * Adds a relationship modifier for the student's opinion of the teacher.
	 * 
	 * @param time the time teaching.
	 */
	private void addRelationshipModifier(double time) {
        RelationshipUtil.changeOpinion(student, person, BASE_RELATIONSHIP_MODIFIER * time);
	}

	@Override
	protected void addExperience(double time) {
        // Add experience to associated skill.
        // (1 base experience point per 100 millisols of time spent)
        double exp = time / 100D;

        // Experience points adjusted by person's "Experience Aptitude" attribute.
//        NaturalAttributeManager nManager = person.getNaturalAttributeManager();
//        int teaching = nManager.getAttribute(NaturalAttributeType.TEACHING);
        double mod = getTeachingExperienceModifier() * 150.0;
        exp *= mod;
        
        if (teachingTask == null)
        	return;
        List<SkillType> taughtSkills = teachingTask.getAssociatedSkills();
        if (taughtSkills == null)
        	return;
        if (!taughtSkills.isEmpty()) {
        	// Pick one skill to improve upon
        	int rand = RandomUtil.getRandomInt(taughtSkills.size()-1);
        	SkillType taskSkill = taughtSkills.get(rand);

			int studentSkill = student.getSkillManager().getSkillLevel(taskSkill);
			double studentExp = student.getSkillManager().getCumuativeExperience(taskSkill);

			int teacherSkill = 0;
			double teacherExp = 0;
			
			if (worker.getUnitType() == UnitType.PERSON) {
				teacherSkill = person.getSkillManager().getSkillLevel(taskSkill);
				teacherExp = person.getSkillManager().getCumuativeExperience(taskSkill);
			}
			else {
				teacherSkill = robot.getSkillManager().getSkillLevel(taskSkill);
				teacherExp = robot.getSkillManager().getCumuativeExperience(taskSkill);
			}
	
			double diff = Math.round((teacherExp - studentExp)*10.0)/10.0;
			int points = teacherSkill - studentSkill;
			double learned = (.5 + points) * exp / 1.5 * RandomUtil.getRandomDouble(1);
			double reward = exp / 40.0 * RandomUtil.getRandomDouble(1);
			
//			logger.info(taskSkill.getName() 
//					+ " - diff: " + diff + "   "
//					+ "  mod: " + mod + "   "
//					+ person + " [Lvl : " + teacherSkill + "]'s teaching reward: " + Math.round(reward*1000.0)/1000.0 
//					+ "   " + student + " [Lvl : " + studentSkill + "]'s learned: " + Math.round(learned*1000.0)/1000.0 + ".");
			
			student.getSkillManager().addExperience(taskSkill, learned, time);
			
			if (worker.getUnitType() == UnitType.PERSON) {
				person.getSkillManager().addExperience(taskSkill, reward, time);
			}
			else
				robot.getSkillManager().addExperience(taskSkill, reward, time);
			
	        // If the student has more experience points than the teacher, the teaching session ends.
	        if (diff < 0)
	        	endTask();
		}
	}

	/**
	 * Gets a collection of the best students the teacher can teach.
	 * 
	 * @param teacher the teacher looking for students.
	 * @return collection of the best students
	 */
	public static Collection<Person> getBestStudents(Person teacher) {
		Collection<Person> result = new ConcurrentLinkedQueue<>();
		Collection<Person> students = getTeachableStudents(teacher);

		// If teacher is in a settlement, best students are in least crowded buildings.
		Collection<Person> leastCrowded = new ConcurrentLinkedQueue<>();
		if (teacher.isInSettlement()) {
			// Find the least crowded buildings that teachable students are in.
			int crowding = Integer.MAX_VALUE;
			Iterator<Person> i = students.iterator();
			while (i.hasNext()) {
				Person student = i.next();
				Building building = BuildingManager.getBuilding(student);
				if (building != null) {
					// If this is an EVA airlock
					if (building.getCategory() == BuildingCategory.EVA_AIRLOCK) {
						// Go to the next building
						continue;
					}
						
					// If this building/hallway is next to the observatory
					if (building.getSettlement().getBuildingManager().isObservatoryAttached(building)) {
						// Go to the next building
						continue;
					}
				
					LifeSupport lifeSupport = building.getLifeSupport();
					int buildingCrowding = lifeSupport.getOccupantNumber() - lifeSupport.getOccupantCapacity() + 1;
					if (buildingCrowding < -1) {
						buildingCrowding = -1;
					}
					if (buildingCrowding < crowding) {
						crowding = buildingCrowding;
					}
				}
			}

			// Add students in least crowded buildings to result.
			Iterator<Person> j = students.iterator();
			while (j.hasNext()) {
				Person student = j.next();
				Building building = BuildingManager.getBuilding(student);
				if (building != null) {
					// If this is an EVA airlock
					if (building.getCategory() == BuildingCategory.EVA_AIRLOCK) {
						// Go to the next building
						continue;
					}
						
					// If this building/hallway is next to the observatory
					if (building.getSettlement().getBuildingManager().isObservatoryAttached(building)) {
						// Go to the next building
						continue;
					}
					
					LifeSupport lifeSupport = building.getLifeSupport();
					int buildingCrowding = lifeSupport.getOccupantNumber() - lifeSupport.getOccupantCapacity() + 1;
					if (buildingCrowding < -1) {
						buildingCrowding = -1;
					}
					if (buildingCrowding == crowding) {
						leastCrowded.add(student);
					}
				}
			}
		} else {
			leastCrowded = students;
		}

		// Get the teacher's favorite students.
		Collection<Person> favoriteStudents = new ConcurrentLinkedQueue<>();

		// Find favorite opinion.
		double favorite = Double.NEGATIVE_INFINITY;
		Iterator<Person> k = leastCrowded.iterator();
		while (k.hasNext()) {
			Person student = k.next();
			double opinion = RelationshipUtil.getOpinionOfPerson(teacher, student);
			if (opinion > favorite) {
				favorite = opinion;
			}
		}

		// Get list of favorite students.
		k = leastCrowded.iterator();
		while (k.hasNext()) {
			Person student = k.next();
			double opinion = RelationshipUtil.getOpinionOfPerson(teacher, student);
			if (opinion == favorite) {
				favoriteStudents.add(student);
			}
		}

		result = favoriteStudents;

		return result;
	}

	/**
	 * Gets a collection of the best students the teacher can teach.
	 * 
	 * @param teacher the teacher looking for students.
	 * @return collection of the best students
	 */
	public static Collection<Person> getBestStudents(Robot teacher) {
		Collection<Person> result = new ConcurrentLinkedQueue<>();
		Collection<Person> students = getTeachableStudents(teacher);

		// If teacher is in a settlement, best students are in least crowded buildings.
		Collection<Person> leastCrowded = new ConcurrentLinkedQueue<>();
		if (teacher.isInSettlement()) {
			// Find the least crowded buildings that teachable students are in.
			int crowding = Integer.MAX_VALUE;
			Iterator<Person> i = students.iterator();
			while (i.hasNext()) {
				Person student = i.next();
				Building building = BuildingManager.getBuilding(student);
				if (building != null) {
					// If this is an EVA airlock
					if (building.getCategory() == BuildingCategory.EVA_AIRLOCK) {
						// Go to the next building
						continue;
					}
						
					// If this building/hallway is next to the observatory
					if (building.getSettlement().getBuildingManager().isObservatoryAttached(building)) {
						// Go to the next building
						continue;
					}
					
					LifeSupport lifeSupport = building.getLifeSupport();
					int buildingCrowding = lifeSupport.getOccupantNumber() - lifeSupport.getOccupantCapacity() + 1;
					if (buildingCrowding < -1) {
						buildingCrowding = -1;
					}
					if (buildingCrowding < crowding) {
						crowding = buildingCrowding;
					}
				}
			}

			// Add students in least crowded buildings to result.
			Iterator<Person> j = students.iterator();
			while (j.hasNext()) {
				Person student = j.next();
				Building building = BuildingManager.getBuilding(student);
				if (building != null) {
					// If this is an EVA airlock
					if (building.getCategory() == BuildingCategory.EVA_AIRLOCK) {
						// Go to the next building
						continue;
					}
						
					// If this building/hallway is next to the observatory
					if (building.getSettlement().getBuildingManager().isObservatoryAttached(building)) {
						// Go to the next building
						continue;
					}
					
					LifeSupport lifeSupport = building.getLifeSupport();
					int buildingCrowding = lifeSupport.getOccupantNumber() - lifeSupport.getOccupantCapacity() + 1;
					if (buildingCrowding < -1) {
						buildingCrowding = -1;
					}
					if (buildingCrowding == crowding) {
						leastCrowded.add(student);
					}
				}
			}
		} else {
			leastCrowded = students;
		}

		result = leastCrowded;

		return result;
	}
	
	/**
	 * Get a collection of students the teacher can teach.
	 * 
	 * @param teacher the teacher looking for students.
	 * @return collection of students
	 */
	private static Collection<Person> getTeachableStudents(Person teacher) {
		Collection<Person> result = new ConcurrentLinkedQueue<>();

		Iterator<Person> i = getLocalPeople(teacher).iterator();
		while (i.hasNext()) {
			Person student = i.next();
			boolean possibleStudent = false;
			Task task = student.getMind().getTaskManager().getTask();
			if (task != null && task.getAssociatedSkills() != null) {
				Iterator<SkillType> j = task.getAssociatedSkills().iterator();
				while (j.hasNext()) {
					SkillType taskSkill = j.next();
					int studentSkill = student.getSkillManager().getSkillLevel(taskSkill);
					int teacherSkill = teacher.getSkillManager().getSkillLevel(taskSkill);
					if ((teacherSkill >= (studentSkill + 1)) && !task.hasTeacher()) {
						possibleStudent = true;
					}
				}
				if (possibleStudent) {
					result.add(student);
				}
			}
		}

		return result;
	}

	/**
	 * Get a collection of students the teacher can teach.
	 * 
	 * @param teacher the teacher looking for students.
	 * @return collection of students
	 */
	private static Collection<Person> getTeachableStudents(Robot teacher) {
		Collection<Person> result = new ConcurrentLinkedQueue<>();

		Iterator<Person> i = getLocalPeople(teacher).iterator();
		while (i.hasNext()) {
			Person student = i.next();
			boolean possibleStudent = false;
			Task task = student.getMind().getTaskManager().getTask();
			if (task != null && task.getAssociatedSkills() != null) {
				Iterator<SkillType> j = task.getAssociatedSkills().iterator();
				while (j.hasNext()) {
					SkillType taskSkill = j.next();
					int studentSkill = student.getSkillManager().getSkillLevel(taskSkill);
					int teacherSkill = teacher.getSkillManager().getSkillLevel(taskSkill);
					if ((teacherSkill >= (studentSkill + 1)) && !task.hasTeacher()) {
						possibleStudent = true;
					}
				}
				if (possibleStudent) {
					result.add(student);
				}
			}
		}

		return result;
	}
	
	
	/**
	 * Gets a collection of people in a person's settlement or rover. The resulting
	 * collection doesn't include the given person.
	 * 
	 * @param person the person checking
	 * @return collection of people
	 */
	private static Collection<Person> getLocalPeople(Person person) {
		Collection<Person> people = new ConcurrentLinkedQueue<>();

		if (person.isInSettlement()) {
			Iterator<Person> i = person.getSettlement().getIndoorPeople().iterator();
			while (i.hasNext()) {
				Person inhabitant = i.next();
				if (person.equals(inhabitant)) {
					people.add(inhabitant);
				}
			}
		} else if (person.isInVehicle()) {
			Crewable rover = (Crewable) person.getVehicle();
			Iterator<Person> i = rover.getCrew().iterator();
			while (i.hasNext()) {
				Person crewmember = i.next();
				if (person.equals(crewmember)) {
					people.add(crewmember);
				}
			}
		}

		return people;
	}

	/**
	 * Gets a collection of robot in a robot's settlement or rover. The resulting
	 * collection doesn't include the given robot.
	 * 
	 * @param robot the robot checking
	 * @return collection of person
	 */
	private static Collection<Person> getLocalPeople(Robot robot) {
		Collection<Person> people = new ConcurrentLinkedQueue<>();

		if (robot.isInSettlement()) {
			Iterator<Person> i = robot.getSettlement().getIndoorPeople().iterator();
			while (i.hasNext()) {
				people.add(i.next());
			}
		} else if (robot.isInVehicle()) {
			Crewable rover = (Crewable) robot.getVehicle();
			Iterator<Person> i = rover.getCrew().iterator();
			while (i.hasNext()) {
				people.add(i.next());
			}
		}

		return people;
	}
}
