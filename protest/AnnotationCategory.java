package protest;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AnnotationCategory {
	public static final int NEW = 0;
	public static final int DONE = 1;
	public static final int CONFLICT = 2;

	public static final int GROUP_COUNT = 3;

	private static String[] groupLabels_ = { "new", "done", "conflict" };
	private static Color[] groupColours_ = { Color.YELLOW, Color.GREEN, Color.RED };

	private int id_;
	private String label_;
	private ArrayList<List<TestSuiteExample>> examples_;

	public AnnotationCategory(int id, String label) {
		id_ = id;
		label_ = label;
		examples_ = new ArrayList<List<TestSuiteExample>>();
		for(int i = 0; i < GROUP_COUNT; i++)
			examples_.add(new ArrayList<TestSuiteExample>());
	}

	public static String getGroupLabel(int grp) {
		return groupLabels_[grp];
	}

	public static Color getGroupColour(int grp) {
		return groupColours_[grp];
	}

	public int getID() {
		return id_;
	}

	public String getLabel() {
		return label_;
	}

	public void addExample(int grp, TestSuiteExample exmpl) {
		examples_.get(grp).add(exmpl);
	}

	public int getCount(int grp) {
		return examples_.get(grp).size();
	}

	public int getCount() {
		int cnt = 0;
		for(List<TestSuiteExample> g : examples_)
			cnt += g.size();
		return cnt;
	}

	public List<TestSuiteExample> getExamples(int grp) {
		return examples_.get(grp);
	}
}

