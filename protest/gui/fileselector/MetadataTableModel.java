package protest.gui.fileselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

public class MetadataTableModel extends AbstractTableModel {
	private ArrayList<String[]> metadata_;

	public MetadataTableModel(HashMap<String,String> metadata) {
		metadata_ = new ArrayList<String[]>();

		for(Map.Entry<String,String> e : metadata.entrySet()) {
			String[] pair = { e.getKey(), e.getValue() };
			metadata_.add(pair);
		}

		Collections.sort(metadata_, new Comparator<String[]>() {
			public int compare(String[] o1, String[] o2) {
				return Arrays.toString(o1).compareTo(Arrays.toString(o2));
			}
		});
	}

	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return metadata_.size();
	}

	public String getValueAt(int row, int col) {
		return metadata_.get(row)[col];
	}
}
