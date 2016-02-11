package protest;

import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

public class MetadataTable extends JPanel {
	private JTable table_;
	private JLabel emptyLabel_;

	public MetadataTable() {
		table_ = new JTable();
		emptyLabel_ = new JLabel("No metadata available");
		add(emptyLabel_);
	}

	public void setDatabase(Database db) {
		HashMap<String,String> metadata;

		removeAll();
		if(db == null || (metadata = db.getMetadata()) == null)
			add(emptyLabel_);
		else {
			table_.setModel(new MetadataTableModel(metadata));
			add(table_);
		}
		revalidate();
		repaint();
	}
}
