/*
 * Mars Simulation Project
 * JHistoryPanel.java
 * @date 2023-07-12
 * @author Barry Evans
 */
package com.mars_sim.ui.swing.utils;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import com.mars_sim.core.data.History;
import com.mars_sim.core.data.History.HistoryItem;
import com.mars_sim.core.time.MarsDate;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.tools.Msg;

/**
 * This is a panel that can display the details of a History object as a table.
 * The panel provides a filters based on teh Mars Sol according to the items in
 * the history.
 */
public abstract class JHistoryPanel<T> extends JPanel {
    /**
	 *
	 */
	private static final int TIME_WIDTH = 90;

	/**
     * Table model that holds the items in the history
     */
	private class ItemModel extends AbstractTableModel {

		private List<HistoryItem<T>> items;
	
		@Override
		public int getRowCount() {
			return (items != null ? items.size() : 0);
		}

		@Override
		public int getColumnCount() {
			return 1 + columnNames.length;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0)
				return String.class;
			return columnTypes[columnIndex-1];
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0)
				return Msg.getString("TabPanelLog.column.time"); //$NON-NLS-1$
			return columnNames[columnIndex-1];
		}

		@Override
		public Object getValueAt(int row, int column) {	
			if (row < items.size()) {
				HistoryItem<T> item = items.get(row);
				if (column == 0) {
					return item.getWhen().getDateTimeStamp();
				} 
				return getValueFrom(item.getWhat(), column-1);
			}
			return null;
		}

		protected void reload() {
			MarsDate selected = (MarsDate) solBox.getSelectedItem();
			items = source.getChanges().stream().filter(i -> i.getWhen().getDate().equals(selected)).toList();
			lastSize = items.size();
			fireTableDataChanged();
		}
	}

	private History<T> source;
	private String[] columnNames;
	private Class<?>[] columnTypes;
	private MarsTime lastTime = null;
	private int lastSize = -1;

	private ItemModel itemModel;
	private JComboBox<MarsDate> solBox;
	private DefaultComboBoxModel<MarsDate> solModel;

    /**
     * This creqtes a panel to display a history details.
     * @param source The source of the History details
     * @param columnNames Name of the extra columns from the Item type
     * @param columnTypes Types of the extra column from the Item type
     */
	protected JHistoryPanel(History<T> source, String[] columnNames, Class<?>[] columnTypes) {
		super(new BorderLayout());
		this.source = source;
		this.columnNames = columnNames;
		this.columnTypes = columnTypes;

		if (columnNames.length != columnTypes.length) {
			throw new IllegalArgumentException("The column names and types to not match");
		}

		solModel = new DefaultComboBoxModel<>();
		solBox = new JComboBox<>(solModel);

		JPanel solPanel = new JPanel(new FlowLayout());
		solPanel.add(solBox);
		add(solPanel, BorderLayout.NORTH);
		itemModel = new ItemModel();

		// Create attribute scroll panel
		JScrollPane scrollPanel = new JScrollPane();
		this.add(scrollPanel, BorderLayout.CENTER);

		// Create schedule table
		JTable table = new JTable(itemModel);
		TableColumn timeColumn = table.getColumnModel().getColumn(0);
		timeColumn.setMinWidth(TIME_WIDTH);
		timeColumn.setPreferredWidth(TIME_WIDTH);

		table.setRowSelectionAllowed(true);

		scrollPanel.setViewportView(table);

		solBox.addActionListener(e -> itemModel.reload());
	}

    /**
     * Refresh the display as data may have changed. 
     * This refresh will only rebuild the table if there has been a materialistic change.
     */
	public void refresh() {
		List<HistoryItem<T>> newItems = source.getChanges();

		// Reload the list if the composition has changed. Either a change of size
		// or the timestamp of 1st item has changed
		if ((lastSize != newItems.size()) 
			|| !newItems.get(0).getWhen().equals(lastTime)) {
			
			Object currentSelection = solBox.getSelectedItem();

			// Reload the sol combo if the range has changed
			List<MarsDate> newRange = source.getRange();
			if (newRange.size() != solModel.getSize()) {
				// Update the solList comboBox
				solModel.removeAllElements();
				solModel.addAll(newRange);
				lastTime = newItems.get(0).getWhen();

				if (currentSelection == null) {
					currentSelection = lastTime.getDate();
				}
				solBox.setSelectedItem(currentSelection);
			}

			// If the new item is visible; then reload table
			MarsDate newItemDate = newItems.get(newItems.size()-1).getWhen().getDate();
			if (newItemDate.equals(currentSelection)) {
				itemModel.reload();
			}
		}
	}

    /**
     * This method must be overriden to extract the appropriate values from the item type.
     * @param value
     * @param columnIndex
     * @return
     */
	protected abstract Object getValueFrom(T value, int columnIndex);
}