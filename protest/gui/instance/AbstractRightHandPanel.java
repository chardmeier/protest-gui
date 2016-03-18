package protest.gui.instance;

import java.awt.LayoutManager;
import javax.swing.JPanel;

import protest.db.AnnotationRecord;

abstract class AbstractRightHandPanel extends JPanel {
	public AbstractRightHandPanel(LayoutManager layout) {
		super(layout);
	}

	abstract void setCurrentAnnotation(AnnotationRecord current);
	abstract public boolean isEditable();
}
