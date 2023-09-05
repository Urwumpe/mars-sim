/*
 * Mars Simulation Project
 * MetaTaskUtil.java
 * @date 2023-06-16
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.ai.task.meta.AnalyzeMapDataMeta;
import org.mars_sim.msp.core.person.ai.task.meta.AssistScientificStudyResearcherMeta;
import org.mars_sim.msp.core.person.ai.task.meta.CompileScientificStudyResultsMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ConnectOnlineMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ConsolidateContainersMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ConstructBuildingMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ConversationMeta;
import org.mars_sim.msp.core.person.ai.task.meta.CookMealMeta;
import org.mars_sim.msp.core.person.ai.task.meta.DelegateWorkMeta;
import org.mars_sim.msp.core.person.ai.task.meta.DigLocalIceMeta;
import org.mars_sim.msp.core.person.ai.task.meta.DigLocalRegolithMeta;
import org.mars_sim.msp.core.person.ai.task.meta.EatDrinkMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ExamineBodyMeta;
import org.mars_sim.msp.core.person.ai.task.meta.InviteStudyCollaboratorMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ListenToMusicMeta;
import org.mars_sim.msp.core.person.ai.task.meta.LoadVehicleMeta;
import org.mars_sim.msp.core.person.ai.task.meta.MaintainBuildingMeta;
import org.mars_sim.msp.core.person.ai.task.meta.MaintainVehicleMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ManufactureConstructionMaterialsMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ManufactureGoodMeta;
import org.mars_sim.msp.core.person.ai.task.meta.MeetTogetherMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ObserveAstronomicalObjectsMeta;
import org.mars_sim.msp.core.person.ai.task.meta.OptimizeSystemMeta;
import org.mars_sim.msp.core.person.ai.task.meta.PeerReviewStudyPaperMeta;
import org.mars_sim.msp.core.person.ai.task.meta.PerformLaboratoryExperimentMeta;
import org.mars_sim.msp.core.person.ai.task.meta.PerformLaboratoryResearchMeta;
import org.mars_sim.msp.core.person.ai.task.meta.PerformMathematicalModelingMeta;
import org.mars_sim.msp.core.person.ai.task.meta.PlanMissionMeta;
import org.mars_sim.msp.core.person.ai.task.meta.PlayHoloGameMeta;
import org.mars_sim.msp.core.person.ai.task.meta.PrepareDessertMeta;
import org.mars_sim.msp.core.person.ai.task.meta.PrescribeMedicationMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ProduceFoodMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ProposeScientificStudyMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ReadMeta;
import org.mars_sim.msp.core.person.ai.task.meta.RecordActivityMeta;
import org.mars_sim.msp.core.person.ai.task.meta.RelaxMeta;
import org.mars_sim.msp.core.person.ai.task.meta.RepairMalfunctionMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ReportMissionControlMeta;
import org.mars_sim.msp.core.person.ai.task.meta.RequestMedicalTreatmentMeta;
import org.mars_sim.msp.core.person.ai.task.meta.RespondToStudyInvitationMeta;
import org.mars_sim.msp.core.person.ai.task.meta.RestingMedicalRecoveryMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ReturnLightUtilityVehicleMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ReviewJobReassignmentMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ReviewMissionPlanMeta;
import org.mars_sim.msp.core.person.ai.task.meta.SalvageBuildingMeta;
import org.mars_sim.msp.core.person.ai.task.meta.SalvageGoodMeta;
import org.mars_sim.msp.core.person.ai.task.meta.SelfTreatHealthProblemMeta;
import org.mars_sim.msp.core.person.ai.task.meta.SleepMeta;
import org.mars_sim.msp.core.person.ai.task.meta.StudyFieldSamplesMeta;
import org.mars_sim.msp.core.person.ai.task.meta.TeachMeta;
import org.mars_sim.msp.core.person.ai.task.meta.TendFishTankMeta;
import org.mars_sim.msp.core.person.ai.task.meta.TendGreenhouseMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ToggleFuelPowerSourceMeta;
import org.mars_sim.msp.core.person.ai.task.meta.ToggleResourceProcessMeta;
import org.mars_sim.msp.core.person.ai.task.meta.TreatMedicalPatientMeta;
import org.mars_sim.msp.core.person.ai.task.meta.UnloadVehicleMeta;
import org.mars_sim.msp.core.person.ai.task.meta.WorkoutMeta;
import org.mars_sim.msp.core.person.ai.task.meta.WriteReportMeta;
import org.mars_sim.msp.core.person.ai.task.meta.YogaMeta;
import org.mars_sim.msp.core.person.ai.task.util.MetaTask.TaskScope;
import org.mars_sim.msp.core.person.ai.task.util.MetaTask.WorkerType;
import org.mars_sim.msp.core.robot.ai.task.ChargeMeta;

/**
 * A utility task for getting the list of meta tasks.
 */
public class MetaTaskUtil {

	private static final String EVA = "EVA";
	private static final String INSIDE = "Inside";
	private static final String GARAGE = "Garage";
	
	private static List<FactoryMetaTask> dutyHourTasks = null;
	private static List<FactoryMetaTask> nonDutyHourTasks = null;

	private static List<FactoryMetaTask> personMetaTasks;
	private static List<FactoryMetaTask> robotMetaTasks = null;

	private static Map<String,MetaTask> idToMetaTask;
	private static List<SettlementMetaTask> settlementTasks;

	/**
	 * Private constructor for utility class.
	 */
	private MetaTaskUtil() {
	}

	/**
	 * Lazy initialisation of metaTasks list.
	 */
	public static synchronized void initializeMetaTasks() {

		if (idToMetaTask != null) {
			// Created by another thread during the wait
			return;
		}
		
		// Would be nice to dynamically load based on what is in the package
		List<MetaTask> allMetaTasks = new ArrayList<>();
		allMetaTasks.add(new AnalyzeMapDataMeta());
		allMetaTasks.add(new AssistScientificStudyResearcherMeta());
		allMetaTasks.add(new ChargeMeta());
		allMetaTasks.add(new CompileScientificStudyResultsMeta());
		allMetaTasks.add(new ConnectOnlineMeta());
		
		allMetaTasks.add(new ConsolidateContainersMeta());
		allMetaTasks.add(new ConstructBuildingMeta());
		allMetaTasks.add(new CookMealMeta());
		allMetaTasks.add(new DelegateWorkMeta());		
		allMetaTasks.add(new DigLocalIceMeta());
		
		allMetaTasks.add(new DigLocalRegolithMeta());
		allMetaTasks.add(new EatDrinkMeta());
		allMetaTasks.add(new ExamineBodyMeta());
		allMetaTasks.add(new ConversationMeta());
		allMetaTasks.add(new InviteStudyCollaboratorMeta());
		
		allMetaTasks.add(new ListenToMusicMeta());
		allMetaTasks.add(new LoadVehicleMeta());
		allMetaTasks.add(new MaintainVehicleMeta());
		allMetaTasks.add(new MaintainBuildingMeta());
		allMetaTasks.add(new ManufactureConstructionMaterialsMeta());
		
		allMetaTasks.add(new ManufactureGoodMeta());
		allMetaTasks.add(new MeetTogetherMeta());
		allMetaTasks.add(new ObserveAstronomicalObjectsMeta());
		allMetaTasks.add(new OptimizeSystemMeta());
		allMetaTasks.add(new PeerReviewStudyPaperMeta());
		
		allMetaTasks.add(new PerformLaboratoryExperimentMeta());
		allMetaTasks.add(new PerformLaboratoryResearchMeta());
		allMetaTasks.add(new PerformMathematicalModelingMeta());
		allMetaTasks.add(new PlanMissionMeta());
		allMetaTasks.add(new PlayHoloGameMeta());
		
		allMetaTasks.add(new PrepareDessertMeta());
		allMetaTasks.add(new PrescribeMedicationMeta());
		allMetaTasks.add(new ProduceFoodMeta());
		allMetaTasks.add(new ProposeScientificStudyMeta());
		allMetaTasks.add(new ReadMeta());
		
		allMetaTasks.add(new RecordActivityMeta());
		allMetaTasks.add(new RelaxMeta());
		allMetaTasks.add(new RepairMalfunctionMeta());
		allMetaTasks.add(new ReportMissionControlMeta());
		allMetaTasks.add(new RequestMedicalTreatmentMeta());
		
		allMetaTasks.add(new RestingMedicalRecoveryMeta());
		allMetaTasks.add(new RespondToStudyInvitationMeta());
		allMetaTasks.add(new ReturnLightUtilityVehicleMeta());
		allMetaTasks.add(new ReviewJobReassignmentMeta());
		allMetaTasks.add(new ReviewMissionPlanMeta());
		
		allMetaTasks.add(new SalvageBuildingMeta());
		allMetaTasks.add(new SalvageGoodMeta());
		allMetaTasks.add(new SelfTreatHealthProblemMeta());
		allMetaTasks.add(new SleepMeta()); 
		allMetaTasks.add(new StudyFieldSamplesMeta());
		
		allMetaTasks.add(new TeachMeta());
		allMetaTasks.add(new TendFishTankMeta());
		allMetaTasks.add(new TendGreenhouseMeta());
		allMetaTasks.add(new ToggleFuelPowerSourceMeta());
		allMetaTasks.add(new ToggleResourceProcessMeta());
		
		allMetaTasks.add(new TreatMedicalPatientMeta());
		allMetaTasks.add(new UnloadVehicleMeta());
		allMetaTasks.add(new WorkoutMeta());
		allMetaTasks.add(new WriteReportMeta());
		allMetaTasks.add(new YogaMeta());
		
		// Build the name lookup for later
		idToMetaTask = new HashMap<>();
		for(MetaTask t : allMetaTasks) {
			idToMetaTask.put(t.getID(), t);
		}

		// Pick put settlement tasks
		settlementTasks = allMetaTasks.stream()
				.filter(m -> (m instanceof SettlementMetaTask))
				.map(s -> (SettlementMetaTask) s)
				.collect(Collectors.toUnmodifiableList());

		// Filter out All Unit Tasks
		personMetaTasks = allMetaTasks.stream()
				.filter(m -> (m instanceof FactoryMetaTask))
				.map(s -> (FactoryMetaTask) s)
				.filter(m -> ((m.getSupported() == WorkerType.BOTH)
								|| (m.getSupported() == WorkerType.PERSON)))
				.collect(Collectors.toUnmodifiableList());
		robotMetaTasks = allMetaTasks.stream()
				.filter(m -> (m instanceof FactoryMetaTask))
				.map(s -> (FactoryMetaTask) s)
				.filter(m -> ((m.getSupported() == WorkerType.BOTH)
								|| (m.getSupported() == WorkerType.ROBOT)))
				.collect(Collectors.toUnmodifiableList());
		
		// Build special Shift based lists
		// Should these be just Person task?
		Map<TaskScope, List<FactoryMetaTask>> metaPerScope = personMetaTasks.stream()
  					.collect(Collectors.groupingBy(MetaTask::getScope));

		List<FactoryMetaTask> tasks = new ArrayList<>();
		tasks.addAll(metaPerScope.get(TaskScope.ANY_HOUR));
		tasks.addAll(metaPerScope.get(TaskScope.WORK_HOUR));
		dutyHourTasks = Collections.unmodifiableList(tasks);

		tasks = new ArrayList<>();
		tasks.addAll(metaPerScope.get(TaskScope.ANY_HOUR));
		tasks.addAll(metaPerScope.get(TaskScope.NONWORK_HOUR));
		nonDutyHourTasks = Collections.unmodifiableList(tasks);
	}

	/**
	 * Gets all the known MetaTasks.
	 * 
	 * @return 
	 */
	public static Collection<MetaTask> getAllMetaTasks() {
		return idToMetaTask.values(); 
	}

	/**
	 * Gets a list of all Person meta tasks.
	 * 
	 * @return list of meta tasks.
	 */
	public static List<FactoryMetaTask> getPersonMetaTasks() {
		return personMetaTasks;
	}

	/**
	 * Gets a list of duty meta tasks.
	 * 
	 * @return list of duty meta tasks.
	 */
	public static List<FactoryMetaTask> getDutyHourTasks() {
		return dutyHourTasks;
	}
	
	/**
	 * Gets a list of non-duty meta tasks.
	 * 
	 * @return list of non-duty meta tasks.
	 */
	public static List<FactoryMetaTask> getNonDutyHourTasks() {
		return nonDutyHourTasks;
	}
	
	
	/**
	 * Get a lists of MetaTasks that are applicable for a Settlement.
	 * 
	 * @return List of SettlementMetaTasks
	 */
    public static List<SettlementMetaTask> getSettlementMetaTasks() {
        return settlementTasks;
    }

	/**
	 * Converts a task name in String to Metatask.
	 * 
	 * @return meta tasks.
	 */
	public static MetaTask getMetaTask(String name) {
		return idToMetaTask.get(name.toUpperCase());
	}

	/**
	 * Gets a MetaTask instance that is associated with a Task class.
	 * Note: this method logic is fragile and needs a better solution.
	 * 
	 * @param task
	 * @return
	 */
	public static MetaTask getMetaTypeFromTask(Task task) {
		String s = task.getClass().getSimpleName();
		String ss = s.replace(EVA, "")
				.replace(INSIDE, "")
				.replace(GARAGE, "");
		String metaTaskName = ss.trim();
	
		return getMetaTask(metaTaskName);
	}

	public static List<FactoryMetaTask> getRobotMetaTasks() {
		return robotMetaTasks;
	}

	/**
	 * Loads any references that MetaTasks need.
	 */
    static void initialiseInstances(Simulation sim) {
		MetaTask.initialiseInstances(sim);
		LoadVehicleMeta.initialiseInstances(sim);
		UnloadVehicleMeta.initialiseInstances(sim);
		ExamineBodyMeta.initialiseInstances(sim.getMedicalManager());
		ReviewMissionPlanMeta.initialiseInstances(sim.getMissionManager());
    }
}
