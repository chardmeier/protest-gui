package protest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.table.AbstractTableModel;

public class TagTableModel extends AbstractTableModel {
	private ArrayList<String> rows_ = new ArrayList<String>();

	public int getRowCount() {
		return rows_.size();
	}

	public int getColumnCount() {
		return 1;
	}

	public String getValueAt(int row, int col) {
		if(col > 0)
			throw new IllegalArgumentException("Trying to access column " + col);
		return rows_.get(row);
	}

	public void setData(Collection<String> c) {
		rows_.clear();
		rows_.addAll(c);
		Collections.sort(rows_);
		fireTableDataChanged();
	}

	public void addTag(String tag) {
		int pos = Collections.binarySearch(rows_, tag);
		if(pos < 0)
			rows_.add(-pos-1, tag);
		fireTableDataChanged();
	}

	public void removeTag(String tag) {
		rows_.remove(tag);
		fireTableDataChanged();
	}
}
