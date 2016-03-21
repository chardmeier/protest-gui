package protest.gui.instance;

import protest.db.TestSuiteExample;

public interface InstanceWindowView {
	public AbstractRightHandPanel getRightHandPanel();
	public String getInstructions();
	public boolean allowTokenEditing();
	public void setCurrentInstance(TestSuiteExample current);
	public void addNavigationListener(NavigationListener l);
}

