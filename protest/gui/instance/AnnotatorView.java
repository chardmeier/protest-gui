package protest.gui.instance;

import java.util.ArrayList;

import protest.db.AnnotationRecord;
import protest.db.TestSuiteExample;

public class AnnotatorView implements InstanceWindowView {
	private int annotator_id_;
	private AnnotationPanel annotationPanel_;

	private ArrayList<NavigationListener> navigationListeners_;

	public AnnotatorView(int annotator_id) {
		annotator_id_ = annotator_id;
		annotationPanel_ = new AnnotationPanel();
		navigationListeners_ = new ArrayList<NavigationListener>();
	}

	public AbstractRightHandPanel getRightHandPanel() {
		return annotationPanel_;
	}

	public String getInstructions() {
		return "<html><b>All pronouns:</b> " +
			"mark whether the pronoun is correctly translated, and select " +
			"the minimum number of tokens necessary for a correct translation.<br>" +
			"<b>Anaphoric pronouns only:</b> mark whether the antecedent head " +
			"is correctly translated, and whether the pronoun translation is correct " +
			"given the antecedent head.<br>" +
			"Select the minimum number of tokens necessary for a correct translation " +
			"of both antecedent and pronoun.</html>";
	}

	public boolean allowTokenEditing() {
		return true;
	}

	public int getDisplayedAnnotatorID() {
		return annotator_id_;
	}

	public void setCurrentInstance(TestSuiteExample current) {
		AnnotationRecord rec = current.getAnnotationRecord(annotator_id_);
		annotationPanel_.setCurrentAnnotation(rec);
		for(NavigationListener l : navigationListeners_)
			l.navigate(current, rec);
	}

	public void addNavigationListener(NavigationListener l) {
		navigationListeners_.add(l);
	}
}
