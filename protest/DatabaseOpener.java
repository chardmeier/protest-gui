package protest;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.sql.SQLException;

import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class DatabaseOpener implements PropertyChangeListener {
	private JFileChooser fileChooser_;
	private MetadataTable metadataTable_;
	
	public DatabaseOpener() {
		fileChooser_ = new JFileChooser(System.getProperty("user.dir"));
		metadataTable_ = new MetadataTable();
		metadataTable_.setPreferredSize(new Dimension(200, 100));
		fileChooser_.setAccessory(new JScrollPane(metadataTable_));
		fileChooser_.addPropertyChangeListener(this);
	}

	public Database open() {
		if(fileChooser_.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return null;
		else
			try {
				return new Database(fileChooser_.getSelectedFile().toString());
			} catch(SQLException e) {
				e.printStackTrace();
				return null;
			}
	}

	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();
		if(prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
			Database db = null;
			File f = (File) e.getNewValue();
			if(f != null && f.isFile()) {
				try {
					db = new Database(f.toString());
				} catch(SQLException ex) {
					// no need to do anything
				}
			}
			metadataTable_.setDatabase(db);
		}
	}
}

