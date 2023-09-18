/**
 * Mars Simulation Project
 * MonitorModel.java
 * @version 3.2.0 2021-06-20
 * @author Manny Kung
 */

package org.mars_sim.msp.ui.swing.tool.monitor;

import javax.swing.table.TableModel;

import org.mars_sim.msp.core.structure.Settlement;

/**
 * This defines a table model for use in the Monitor tool.
 * The subclasses on this model could provide data on any Entity within the
 * Simulation. This interface defines simple extra method that provide a richer
 * interface for the Monitor window to be based upon.
 */
interface MonitorModel extends TableModel {

	/**
	 * Get the name of this model. The name will be a description helping
	 * the user understand the contents.
	 * @return Descriptive name.
	 */
	public String getName();


	/**
	 * Return the object at the specified row indexes.
	 * @param row Index of the row object.
	 * @return Object at the specified row.
	 */
	public Object getObject(int row);

	/**
	 * Has this model got a natural order that the model conforms to. If this
	 * value is true, then it implies that the user should not be allowed to
	 * order.
	 */
	public boolean getOrdered();

	/**
	 * Prepares the model for deletion.
	 */
	public void destroy();

	/**
	 * Gets the model count string.
	 */
	public String getCountString();

	/**
	 * Set the Settlement as a filter
	 * @param filter Settlement
	 * @return 
	 */
	public boolean setSettlementFilter(Settlement filter);

	/**
	 * Set whether the changes to the Entities should be monitor for change.
	 * @param activate 
	 */
    public void setMonitorEntites(boolean activate);

	/**
	 * Get a tooltip representation of a cell. Most cells with return null.
	 * @param rowIndex
	 * @param colIndex
	 * @return
	 */
    public String getToolTipAt(int rowIndex, int colIndex);
}
