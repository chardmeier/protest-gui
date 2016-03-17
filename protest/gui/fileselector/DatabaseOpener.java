package protest.gui.fileselector;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import protest.db.Database;
import protest.db.DatabaseException;

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

	public String open() {
		if(fileChooser_.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return null;
		else
			return (fileChooser_.getSelectedFile().toString());
	}

	public String save() {
		if(fileChooser_.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
			return null;
		else
			return fileChooser_.getSelectedFile().toString();
	}

	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();
		if(prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
			Database db = null;
			File f = (File) e.getNewValue();
			if(f != null && f.isFile()) {
				try {
					db = new Database(f.toString());
				} catch(DatabaseException ex) {
					// no need to do anything
				}
			}
			metadataTable_.setDatabase(db);
		}
	}
}

