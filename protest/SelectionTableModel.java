package protest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.table.AbstractTableModel;

public class SelectionTableModel extends AbstractTableModel {
	private ArrayList<String> items_ = new ArrayList<String>();
	private ArrayList<Integer> counts_ = new ArrayList<Integer>();

	public void add(String label, int count) {
		items_.add(label);
		counts_.add(Integer.valueOf(count));
	}

	public void resetCounts() {
		Collections.fill(counts_, Integer.valueOf(0));
	}

	public void setCount(String label, int count) {
		int pos = items_.indexOf(label);
		counts_.set(pos, Integer.valueOf(count));
	}

	public int getRowCount() {
		return items_.size();
	}

	public int getColumnCount() {
		return 2;
	}

	public Object getValueAt(int row, int col) {
		switch(col) {
		case 0:
			return items_.get(row);
		case 1:
			return counts_.get(row);
		default:
			throw new IllegalArgumentException("Undefined column number: " + row);
		}
	}
}
