/*
 * Mars Simulation Project
 * AbstractTaskJob.java
 * @date 2022-11-11
 * @author Barry Evans
 */
package org.mars_sim.msp.core.person.ai.task.util;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.robot.Robot;

/**
 * Abstract implement of a Task Job just holding the description & score.
 */
public abstract class AbstractTaskJob implements TaskJob {

	private static final long serialVersionUID = 1L;
	
	private static final double CAP = 1000D;
    private double score;
    private String description;

    protected AbstractTaskJob(String description, double score) {
    	this.description = description;
  
        if (score > CAP) {
            score = CAP;
        }
        this.score = score;
    }

    @Override
    public double getScore() {
        return score;
    }

    @Override
    public String getDescription() {
        return description;
    }  

    /**
     * Default implementation throws an Unsupported operation exception.
     */
    @Override
    public Task createTask(Robot robot) {
        throw new UnsupportedOperationException("Robots cannot do " + description);
    }

    /**
     * Default implementation throws an Unsupported operation exception.
     * 
	 * @param person the person to perform the task.
	 * @return task instance.
     */
    @Override
    public Task createTask(Person person) {
        throw new UnsupportedOperationException("Persons cannot do " + description);
    }
    
    /**
	 * Default implementation throws an Unsupported operation exception.
	 * 
	 * @param person the person to perform the task.
	 * @param duration
	 * @return task instance.
	 */
    public Task createTask(Person person, int duration) {
        throw new UnsupportedOperationException("Persons cannot do " + description);
    }
    
    @Override
    public String toString() {
        return getDescription();
    }

}
