package protest.gui.instance;

import protest.db.AnnotationRecord;
import protest.db.TestSuiteExample;

public interface NavigationListener {
	public void navigate(TestSuiteExample example, AnnotationRecord rec);
}
