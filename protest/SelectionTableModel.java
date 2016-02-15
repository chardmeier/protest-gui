package protest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.table.AbstractTableModel;

public class SelectionTableModel extends AbstractTableModel {
	private ArrayList<String> items_;
	private ArrayList<Integer> counts_;

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
}
