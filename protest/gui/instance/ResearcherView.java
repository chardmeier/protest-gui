package protest.gui.instance;

import java.awt.Component;

import protest.db.TestSuiteExample;

public class ResearcherView implements InstanceWindowView {
	private OverviewPanel overviewPanel_;

	public ResearcherView(int annotator_id) {
		overviewPanel_ = new OverviewPanel(annotator_id);
	}

	public Component getRightHandPanel() {
		return overviewPanel_;
	}

	public String getInstructions() {
		return "";
	}

	public boolean allowTokenEditing() {
		return false;
	}

	public void setCurrentInstance(TestSuiteExample current) {
		overviewPanel_.setCurrentInstance(current);
	}

	public void addNavigationListener(NavigationListener l) {
		overviewPanel_.addNavigationListener(l);
	}
}
