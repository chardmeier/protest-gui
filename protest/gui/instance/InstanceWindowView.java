package protest.gui.instance;

import java.awt.Component;

import protest.db.TestSuiteExample;

public interface InstanceWindowView {
	public Component getRightHandPanel();
	public String getInstructions();
	public boolean allowTokenEditing();
	public void setCurrentInstance(TestSuiteExample current);
	public void addNavigationListener(NavigationListener l);
}

