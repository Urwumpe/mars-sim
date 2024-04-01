/*
 * Mars Simulation Project
 * TabPanelTable.java
 * @date 2024-03-31
 * @author Barry Evans
 */
package com.mars_sim.ui.swing.unit_window;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.mars_sim.core.Unit;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.utils.UnitModel;
import com.mars_sim.ui.swing.utils.UnitTableLauncher;

/**
 * This is a tab panel for display a table and a information panel
 */
@SuppressWarnings("serial")
public abstract class TabPanelTable extends TabPanel {
	
	// implementation code to set a tooltip text to each column of JTableHeader
	private static class ToolTipHeader extends JTableHeader {
		String[] toolTips;
		
		public ToolTipHeader(TableColumnModel model) {
			super(model);
		}
		
		public String getToolTipText(MouseEvent e) {
			int col = columnAtPoint(e.getPoint());
			int modelCol = getTable().convertColumnIndexToModel(col);
			String retStr;
			try {
				retStr = toolTips[modelCol];
			} catch (NullPointerException ex) {
				retStr = "";
			} catch (ArrayIndexOutOfBoundsException ex) {
				retStr = "";
			}
			if (retStr.length() < 1) {
				retStr = super.getToolTipText(e);
			}
			return retStr;
		}
		
		public void setToolTipStrings(String[] toolTips) {
			this.toolTips = toolTips;
		}
	}

	private JScrollPane scrollPane;
	private String[] headerTooltips;
	
	/**
	 * Constructor.
	 * 
	 * @param tabTitle   the title to be displayed in the tab (may be null).
	 * @param tabIcon    the icon to be displayed in the tab (may be null).
	 * @param tabToolTip the tool tip to be displayed in the icon (may be null).
	 * @param desktop    the main desktop.
	 */
	protected TabPanelTable(String tabTitle, Icon tabIcon, String tabToolTip, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(tabTitle, tabIcon, tabToolTip, desktop);
	}
	
	/**
	 * Add some tooltips to the tabe header
	 * @param tooltips
	 */
	protected void setHeaderToolTips(String [] tooltips) {
		headerTooltips = tooltips;
	}

	/**
	 * Constructor when there is an associated Unit.
	 *
	 * @param tabTitle   the title to be displayed in the tab (may be null).
	 * @param tabIcon    the icon to be displayed in the tab (may be null).
	 * @param tabToolTip the tool tip to be displayed in the icon (may be null).
	 * @param unit       the unit to display.
	 * @param desktop    the main desktop.
	 */
	protected TabPanelTable(String tabTitle, Icon tabIcon, String tabToolTip, Unit unit, MainDesktopPane desktop) {
		super(tabTitle, tabIcon, tabToolTip, unit, desktop);
	}

	@Override
	protected void buildUI(JPanel content) {
		
		// Prepare  info panel.
		JPanel infoPanel = createInfoPanel();
		if (infoPanel != null) {
			content.add(infoPanel, BorderLayout.NORTH);
		}

		// Create scroll panel for the outer table panel.
		scrollPane = new JScrollPane();
		// increase vertical mousewheel scrolling speed for this one
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		content.add(scrollPane,BorderLayout.CENTER);
		
		// Prepare table model.
		var tableModel = createModel();
		
		// Prepare table.
		var table = new JTable(tableModel);
		if (tableModel instanceof UnitModel) {
			// Call up the window when clicking on a row on the table
			table.addMouseListener(new UnitTableLauncher(getDesktop()));
		}
		
		table.setRowSelectionAllowed(true);
		var tc = table.getColumnModel();
		setColumnDetails(tc);
		
				
		// Set up tooltips for the column headers
		if (headerTooltips != null) {
			ToolTipHeader tooltipHeader = new ToolTipHeader(tc);
			tooltipHeader.setToolTipStrings(headerTooltips);
			table.setTableHeader(tooltipHeader);
		}

		// Resizable automatically when its Panel resizes
		table.setPreferredScrollableViewportSize(new Dimension(225, -1));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		// Add sorting
		table.setAutoCreateRowSorter(true);

		scrollPane.setViewportView(table);
	}

	/**
	 * This method should configure the table for any special renderers or
	 * column widths. It should be overriden by subclasses.
	 * @param columnModel Columns to be configured
	 */
	protected void setColumnDetails(TableColumnModel columnModel) {
		// Default implementation does nothing
	}

	/**
	 * This is for the model to be dispalyed in the table.
	 * If the return is a UnitModel then the launcher will be enabled.
	 * @return
	 */
	protected abstract TableModel createModel();

	/**
	 * If the panel has an info panel then it is created by this method. It will be placed
	 * above the table.
	 * Subclasses can override this if info is to be displayed
	 * @return Could return null
	 */
	protected JPanel createInfoPanel() {
		return null;
	}
}
