/*
 * Mars Simulation Project
 * UnitTableLauncher.java
 * @date 2023-02-21
 * @author Barry Evans
 */
package com.mars_sim.ui.swing.utils;

import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableModel;

import com.mars_sim.ui.swing.MainDesktopPane;

/**
 * This class listens for double click event on a JTable. When an event triggers; a UnitDetail window
 * is launched. The JTable has to be using a UnitTable model that allows the associated Unit
 * of the selected Row.
 * 
 * @see UnitModel
 */
public class UnitTableLauncher extends MouseInputAdapter {
    private MainDesktopPane desktop;

    /**
     * Creates a launcher that will create a UnitDetail window.
     * 
     * @param desktop
     */
    public UnitTableLauncher(MainDesktopPane desktop) {
        this.desktop = desktop;
    }

    /**
     * Catches the double click mouse event. The component under the click event is retrieved
     * which should be a JTable; from this the assigned UnitModel is used to find the
     * associated Unit. This desktop is that used to open the appropriate Unit window.
     * This method supports the JTable being sorted.
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && !e.isConsumed()) {
            JTable table = (JTable) e.getComponent();
            // Get the mouse-selected row
            int r = table.getSelectedRow();
            RowSorter<? extends TableModel> sorter = table.getRowSorter();
            if (sorter != null && r >= 0) {
                r = sorter.convertRowIndexToModel(r);
            }

            UnitModel model = (UnitModel)table.getModel();
            desktop.showDetails(model.getAssociatedUnit(r));
        }
    }
}
