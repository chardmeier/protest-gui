package protest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.table.AbstractTableModel;

public class SelectionTableModel extends AbstractTableModel {
	private ArrayList<Integer> ids_ = new ArrayList<Integer>();
	private ArrayList<String> items_ = new ArrayList<String>();
	private ArrayList<Integer> counts_ = new ArrayList<Integer>();

	public void add(int id, String label, int count) {
		ids_.add(Integer.valueOf(id));
		items_.add(label);
		counts_.add(Integer.valueOf(count));
	}

	public void resetCounts() {
		Collections.fill(counts_, Integer.valueOf(0));
		fireTableDataChanged();
	}

	public void setCount(String label, int count) {
		int pos = items_.indexOf(label);
		counts_.set(pos, Integer.valueOf(count));
		fireTableCellUpdated(pos, 1);
	}

	public int[] translateSelection(int[] sel) {
		int[] res = new int[sel.length];
		for(int i = 0; i < sel.length; i++)
			res[i] = ids_.get(sel[i]).intValue();
		return res;
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
