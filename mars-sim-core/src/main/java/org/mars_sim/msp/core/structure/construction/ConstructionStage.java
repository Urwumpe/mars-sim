/**
 * Mars Simulation Project
 * ConstructionStage.java
 * @date 2023-06-07
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.construction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.tool.Conversion;

/**
 * A construction stage of a construction site.
 * TODO externalize strings
 */
public class ConstructionStage implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    // Construction site events.
    public static final String ADD_CONSTRUCTION_WORK_EVENT = "adding construction work";
    public static final String ADD_SALVAGE_WORK_EVENT = "adding salvage work";
    public static final String ADD_CONSTRUCTION_MATERIALS_EVENT = "adding construction materials";

    /** Work time modifier for salvaging a construction stage. */
    private static final double SALVAGE_WORK_TIME_MODIFIER = .25D;

    // Data members
    private boolean isSalvaging;
    
    private double completedWorkTime;
    private double completableWorkTime;

    private ConstructionStageInfo info;
    private ConstructionSite site;
    
    private Map<Integer, Integer> missingParts;
    private Map<Integer, Double> missingResources;

    private Map<Integer, Integer> originalReqParts;
    private Map<Integer, Double> originalReqResources;
    
//    private Map<Integer, Integer> consumedParts;
//    private Map<Integer, Double> consumedResources;
    
    /**
     * Constructor.
     * 
     * @param info the stage information.
     */
    public ConstructionStage(ConstructionStageInfo info, ConstructionSite site) {
        this.info = info;
        this.site = site;
        
        isSalvaging = false;
        
        completedWorkTime = 0D;
        completableWorkTime = 0D;
        
        originalReqParts = new HashMap<>(info.getParts());
        originalReqResources = new HashMap<>(info.getResources());

        missingParts = new HashMap<>(info.getParts());
        missingResources = new HashMap<>(info.getResources());
        
        // Update the remaining completable work time.
        updateCompletableWorkTime();
    }

    /**
     * Get the construction stage information.
     * 
     * @return stage information.
     */
    public ConstructionStageInfo getInfo() {
        return info;
    }

    /**
     * Gets the completed work time on the stage.
     * 
     * @return work time (in millisols).
     */
    public double getCompletedWorkTime() {
        return completedWorkTime;
    }

    /**
     * Sets the completed work time on the stage.
     * 
     * @param completedWorkTime work time (in millisols).
     */
    public void setCompletedWorkTime(double completedWorkTime) {
        this.completedWorkTime = completedWorkTime;
    }

    /**
     * Gets the amount work time that can be completed for this stage.
     * 
     * @return completable work time (millisols).
     */
    public double getCompletableWorkTime() {
        return completableWorkTime;
    }

    /**
     * Gets the required work time for the stage.
     * 
     * @return work time (in millisols).
     */
    public double getRequiredWorkTime() {
        double requiredWorkTime = info.getWorkTime();
        if (isSalvaging) {
            requiredWorkTime *= SALVAGE_WORK_TIME_MODIFIER;
        }
        return requiredWorkTime;
    }

    /**
     * Adds work time to the construction stage.
     * 
     * @param workTime the work time (in millisols) to add.
     */
    public void addWorkTime(double workTime) {
        completedWorkTime += workTime;

        if (completedWorkTime > getRequiredWorkTime()) {
            completedWorkTime = getRequiredWorkTime();
        }

        // Fire construction event
        if (isSalvaging) {
            site.fireConstructionUpdate(ADD_SALVAGE_WORK_EVENT, this);
        }
        else {
            site.fireConstructionUpdate(ADD_CONSTRUCTION_WORK_EVENT, this);
        }
    }

    /**
     * Checks if the stage is complete.
     * 
     * @return true if stage is complete.
     */
    public boolean isComplete() {
        return (completedWorkTime >= getRequiredWorkTime());
    }

    /**
     * Checks if the stage is salvaging.
     * 
     * @return true if stage is salvaging.
     */
    public boolean isSalvaging() {
        return isSalvaging;
    }

    /**
     * Sets if the stage is salvaging.
     * 
     * @param isSalvaging true if staging is salvaging.
     */
    public void setSalvaging(boolean isSalvaging) {
        this.isSalvaging = isSalvaging;
    }

    /**
     * Gets the original parts needed for construction.
     * 
     * @return map of parts and their numbers.
     */
    public Map<Integer, Integer> getOriginalParts() {
        return new HashMap<>(originalReqParts);
    }

    /**
     * Gets the original resources needed for construction.
     * 
     * @return map of resources and their amounts (kg).
     */
    public Map<Integer, Double> getOriginalResources() {
        return new HashMap<>(originalReqResources);
    }

    /**
     * Gets the remaining parts needed for construction.
     * 
     * @return map of parts and their numbers.
     */
    public Map<Integer, Integer> getRemainingParts() {
        return new HashMap<>(missingParts);
    }

    /**
     * Gets the remaining resources needed for construction.
     * 
     * @return map of resources and their amounts (kg).
     */
    public Map<Integer, Double> getRemainingResources() {
        return new HashMap<>(missingResources);
    }
    
    /**
     * Adds parts to the construction stage.
     * 
     * @param part the part to add.
     * @param number the number of parts to add.
     */
    public void addParts(Integer part, int number) {

        if (missingParts.containsKey(part)) {
            int remainingRequiredNum = missingParts.get(part);
            if (number <= remainingRequiredNum) {
                remainingRequiredNum -= number;
                if (remainingRequiredNum > 0) {
                    missingParts.put(part, remainingRequiredNum);
                }
                else {
                    missingParts.remove(part);
                }

                // Update the remaining completable work time.
                updateCompletableWorkTime();
                
                // Fire construction event
                site.fireConstructionUpdate(ADD_CONSTRUCTION_MATERIALS_EVENT, this);
            }
            else {
                throw new IllegalStateException("Trying to add " + number + " " + part + 
                        " to " + info.getName() + " when only " + remainingRequiredNum + 
                        " are needed.");
            }
        }
        else {
            throw new IllegalStateException("Construction stage " + info.getName() + 
                    " does not require part " + part);
        }
    }

    /**
     * Adds resource to the construction stage.
     * 
     * @param resource the resource to add.
     * @param amount the amount (kg) of resource to add.
     */
    public void addResource(Integer resource, double amount) {

        if (missingResources.containsKey(resource)) {
            double remainingRequiredAmount = missingResources.get(resource);
            if (amount <= remainingRequiredAmount) {
                remainingRequiredAmount -= amount;
//                if (remainingRequiredAmount > 0D) {
                    missingResources.put(resource, remainingRequiredAmount);
//                }
//                else {
//                    missingResources.remove(resource);
//                }

                // Update the remaining completable work time.
                updateCompletableWorkTime();
                
                // Fire construction event
                site.fireConstructionUpdate(ADD_CONSTRUCTION_MATERIALS_EVENT, this);
            }
            else {
                throw new IllegalStateException("Trying to add " + amount + " " + resource + 
                        " to " + info.getName() + " when only " + remainingRequiredAmount + 
                        " are needed.");
            }
        }
        else {
            throw new IllegalStateException("Construction stage " + info.getName() + 
                    " does not require resource " + resource);
        }
    }

    /**
     * Updates the completable work time available.
     */
    private void updateCompletableWorkTime() {

        double totalRequiredConstructionMaterial = getConstructionMaterialMass(
                info.getResources(), info.getParts());

        double totalMissingConstructionMaterial = getConstructionMaterialMass(
                missingResources, missingParts);

        double proportion = 1D;
        if (totalRequiredConstructionMaterial > 0D) {
            proportion = (totalRequiredConstructionMaterial - totalMissingConstructionMaterial) / 
                    totalRequiredConstructionMaterial;
        }

        completableWorkTime = proportion * info.getWorkTime();
    }

    /**
     * Gets the total mass of construction materials.
     * 
     * @param resources map of resources and their amounts (kg).
     * @param parts map of parts and their numbers.
     * @return total mass.
     */
    private double getConstructionMaterialMass(Map<Integer, Double> resources, 
    		Map<Integer, Integer> parts) {

        double result = 0D;

        // Add total mass of resources.
        Iterator<Integer> i = resources.keySet().iterator();
        while (i.hasNext()) {
        	Integer resource = i.next();
            double amount = resources.get(resource);
            result += amount;
        }

        // Add total mass of parts.
        Iterator<Integer> j = parts.keySet().iterator();
        while (j.hasNext()) {
        	Integer part = j.next();
            int number = parts.get(part);
            double mass = ItemResourceUtil.findItemResource(part).getMassPerItem();
            result += number * mass;
        }

        return result;
    }

    @Override
    public String toString() {
        String result = "";
        if (isSalvaging) result = Conversion.capitalize("Salvaging " + info.getName());
        else result = Conversion.capitalize("Constructing " + info.getName());
        return result;
    }
    
	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		info = null;
	    site = null;
	    missingParts.clear();
	    missingResources.clear();
	    originalReqParts.clear();
	    originalReqResources.clear();
	    
	    missingParts = null;
	    missingResources = null;
	    originalReqParts = null;
	    originalReqResources = null;
	}
}
