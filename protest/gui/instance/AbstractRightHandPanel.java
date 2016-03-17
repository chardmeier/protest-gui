package protest.gui.instance;

import java.awt.LayoutManager;
import javax.swing.JPanel;

import protest.db.TestSuiteExample;

abstract class AbstractRightHandPanel extends JPanel {
	public AbstractRightHandPanel(LayoutManager layout) {
		super(layout);
	}

	abstract void setCurrentInstance(TestSuiteExample current);
	abstract public boolean isEditable();
}
