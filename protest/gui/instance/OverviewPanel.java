package protest.gui.instance;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import protest.db.AnnotationRecord;
import protest.db.TestSuiteExample;

class OverviewPanel extends JPanel implements ActionListener {
	private BrowsePanel annotationBrowser_;
	private JTable annotationTable_;
	private JTable tagTable_;
	private JTextArea remarksField_;

	private TestSuiteExample current_;
	private List<AnnotationRecord> annotationRecords_;
	private AnnotationRecord currentAnnotation_;

	private ArrayList<NavigationListener> navigationListeners_;

	public OverviewPanel() {
		super(new BorderLayout());
		
		navigationListeners_ = new ArrayList<NavigationListener>();

		JPanel upperPanel = new JPanel(new BorderLayout());

		annotationBrowser_ = new BrowsePanel("annot");
		annotationBrowser_.addActionListener(this);
		upperPanel.add(annotationBrowser_, BorderLayout.PAGE_START);

		annotationTable_ = new JTable();
		annotationTable_.setPreferredSize(new Dimension(300, 180));
		annotationTable_.setBorder(BorderFactory.createTitledBorder("Annotations:"));
		upperPanel.add(new JScrollPane(annotationTable_), BorderLayout.CENTER);

		tagTable_ = new JTable();
		tagTable_.setPreferredSize(new Dimension(300, 180));
		tagTable_.setBorder(BorderFactory.createTitledBorder("Tags:"));
		upperPanel.add(new JScrollPane(tagTable_), BorderLayout.PAGE_END);

		remarksField_ = new JTextArea(10, 30);
		remarksField_.setBorder(BorderFactory.createTitledBorder("Remarks:"));
		remarksField_.setEditable(false);
		remarksField_.setLineWrap(true);
		remarksField_.setWrapStyleWord(true);

		this.add(upperPanel, BorderLayout.PAGE_START);
		this.add(remarksField_, BorderLayout.CENTER);
	}

	public void setCurrentInstance(TestSuiteExample current) {
		current_ = current;
		annotationRecords_ = current_.getAnnotationRecords();
		annotationBrowser_.setTotal(annotationRecords_.size());
		if(!annotationRecords_.isEmpty())
			currentAnnotation_ = annotationRecords_.get(0);
		else
			currentAnnotation_ = null;

		DefaultTableColumnModel colModel = new DefaultTableColumnModel();
		TickCellRenderer renderer = new TickCellRenderer();
		colModel.addColumn(new TableColumn(0, 75));
		for(int i = 0; i < annotationRecords_.size(); i++)
			colModel.addColumn(new TableColumn(i + 1, 20, renderer, null));

		DefaultTableModel annotationTableModel = new DefaultTableModel();
		annotationTableModel.addColumn("correct?", new String[] { "antecedent", "pronoun" });
		for(AnnotationRecord rec : annotationRecords_) {
			annotationTableModel.addColumn(rec.getAnnotatorName(),
					new String[] { rec.getAntecedentAnnotation(),
						rec.getAnaphorAnnotation() });
		}
		annotationTable_.setModel(annotationTableModel);
		annotationTable_.setColumnModel(colModel);

		TreeSet<String> allTags = new TreeSet<String>();
		for(AnnotationRecord rec : annotationRecords_)
			allTags.addAll(rec.getTags());

		DefaultTableModel tagTableModel = new DefaultTableModel();
		tagTableModel.addColumn("Tag", allTags.toArray());
		for(AnnotationRecord rec : annotationRecords_) {
			Set<String> recTags = rec.getTags();
			ArrayList<String> ticks = new ArrayList<String>();
			for(String tag : allTags) {
				if(recTags.contains(tag))
					ticks.add("ok");
				else
					ticks.add("");
			}
			tagTableModel.addColumn(rec.getAnnotatorName(), ticks.toArray());
		}
		tagTable_.setModel(tagTableModel);
		tagTable_.setColumnModel(colModel);

		fireNavigationEvent();
	}

	public void actionPerformed(ActionEvent e) {
		String[] cmd = e.getActionCommand().split(" ");
		if(cmd[0].equals("annot")) {
			int idx = Integer.parseInt(cmd[1]);
			currentAnnotation_ = annotationRecords_.get(idx);
			fireNavigationEvent();
		}
	}

	public void addNavigationListener(NavigationListener l) {
		navigationListeners_.add(l);
	}

	private void fireNavigationEvent() {
		for(NavigationListener l : navigationListeners_)
			l.navigate(current_, currentAnnotation_);
	}
}
