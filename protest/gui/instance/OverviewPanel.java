package protest.gui.instance;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import protest.db.AnnotationRecord;
import protest.db.TestSuiteExample;

class OverviewPanel extends JPanel implements ActionListener, BrowsingListener {
	private final static Color HEADER_COLOR = new Color(235, 235, 235);

	// the researcher's annotator ID, for adding tags
	private int annotator_id_;

	private BrowsePanel annotationBrowser_;
	private JTable annotationTable_;
	private JTable tagTable_;
	private JComboBox newTag_;
	private DefaultComboBoxModel newTagModel_;
	private JTextArea remarksField_;

	private TestSuiteExample current_;
	private List<AnnotationRecord> annotationRecords_;
	private AnnotationRecord currentAnnotation_;

	private ArrayList<NavigationListener> navigationListeners_;

	public OverviewPanel(int annotator_id) {
		super(new BorderLayout());
		
		annotator_id_ = annotator_id;

		navigationListeners_ = new ArrayList<NavigationListener>();

		JPanel upperPanel = new JPanel(new BorderLayout());

		annotationBrowser_ = new BrowsePanel("annot");
		annotationBrowser_.setBrowsingListener(this);
		upperPanel.add(annotationBrowser_, BorderLayout.PAGE_START);

		JPanel annotationAndTagPanel = new JPanel(new BorderLayout());
		upperPanel.add(annotationAndTagPanel, BorderLayout.CENTER);

		JPanel annotationPanel = new JPanel(new BorderLayout());
		annotationTable_ = new JTable();
		annotationTable_.setAutoCreateColumnsFromModel(false);
		JTableHeader annotationHeader = annotationTable_.getTableHeader();
		annotationHeader.setBackground(HEADER_COLOR);
		annotationPanel.add(annotationHeader, BorderLayout.PAGE_START);
		annotationPanel.add(annotationTable_, BorderLayout.CENTER);
		Border annotationBorder = BorderFactory.createTitledBorder("Annotations:");
		annotationPanel.setBorder(annotationBorder);
		Insets ins = annotationBorder.getBorderInsets(annotationTable_);
		annotationTable_.setPreferredSize(new Dimension(300 - ins.left - ins.right,
					2 * annotationTable_.getRowHeight() + 1));
		annotationAndTagPanel.add(annotationPanel, BorderLayout.CENTER);

		JPanel tagPanel = new JPanel(new BorderLayout());
		tagPanel.setPreferredSize(new Dimension(300, 180));
		tagPanel.setBorder(BorderFactory.createTitledBorder("Tags:"));

		tagTable_ = new JTable();
		tagTable_.setAutoCreateColumnsFromModel(false);
		tagTable_.getTableHeader().setBackground(HEADER_COLOR);
		tagPanel.add(new JScrollPane(tagTable_));

		JPanel newTagPanel = new JPanel();
		newTagModel_ = new DefaultComboBoxModel();
		newTag_ = new JComboBox(newTagModel_);
		newTag_.setEditable(true);
		newTag_.setPrototypeDisplayValue("123456789");
		JButton removeTagButton = new JButton("-");
		removeTagButton.setActionCommand("remove-tag");
		removeTagButton.addActionListener(this);
		JButton addTagButton = new JButton("+");
		addTagButton.setActionCommand("add-tag");
		addTagButton.addActionListener(this);
		newTagPanel.add(removeTagButton);
		newTagPanel.add(newTag_);
		newTagPanel.add(addTagButton);
		tagPanel.add(newTagPanel, BorderLayout.PAGE_END);

		annotationAndTagPanel.add(tagPanel, BorderLayout.PAGE_END);

		remarksField_ = new JTextArea(10, 30);
		remarksField_.setBorder(BorderFactory.createTitledBorder("Remarks:"));
		remarksField_.setEditable(false);
		remarksField_.setLineWrap(true);
		remarksField_.setWrapStyleWord(true);
		remarksField_.setPreferredSize(new Dimension(300, 180));

		this.add(upperPanel, BorderLayout.PAGE_START);
		this.add(new JScrollPane(remarksField_), BorderLayout.CENTER);
	}

	public void setCurrentInstance(TestSuiteExample current) {
		current_ = current;
		annotationRecords_ = current_.getAnnotationRecords();
		annotationBrowser_.setTotal(annotationRecords_.size());
		if(!annotationRecords_.isEmpty())
			currentAnnotation_ = annotationRecords_.get(0);
		else
			currentAnnotation_ = null;

		DefaultTableModel annotationTableModel = new DefaultTableModel();
		if(current_.getAntecedentAgreementRequired()) {
			annotationTableModel.addColumn("correct?", new String[] { "antecedent", "pronoun" });
			for(AnnotationRecord rec : annotationRecords_) {
				annotationTableModel.addColumn(rec.getAnnotatorName(),
						new String[] { rec.getAntecedentAnnotation(),
							rec.getAnaphorAnnotation() });
			}
		} else {
			annotationTableModel.addColumn("correct?", new String[] { "pronoun" });
			for(AnnotationRecord rec : annotationRecords_) {
				annotationTableModel.addColumn(rec.getAnnotatorName(),
						new String[] { rec.getAnaphorAnnotation() });
			}
		}
		annotationTable_.setModel(annotationTableModel);
		annotationTable_.setColumnModel(makeAnnotatorColumnModel());
		annotationTable_.getTableHeader().repaint();

		displayTagList();

		StringBuilder allRemarks = new StringBuilder();
		for(AnnotationRecord rec : annotationRecords_) {
			allRemarks.append("Annotator " + rec.getAnnotatorName() + ":\n\n");
			allRemarks.append(rec.getRemarks());
			allRemarks.append("\n\n\n");
		}
		remarksField_.setText(allRemarks.toString());

		fireNavigationEvent();
	}

	private DefaultTableColumnModel makeAnnotatorColumnModel() {
		DefaultTableColumnModel colModel = new DefaultTableColumnModel();
		TickCellRenderer renderer = new TickCellRenderer();
		TableColumn annotatorColumn = new TableColumn(0, 75);
		annotatorColumn.setHeaderValue("");
		colModel.addColumn(annotatorColumn);
		for(int i = 0; i < annotationRecords_.size(); i++) {
			TableColumn col = new TableColumn(i + 1, 20, renderer, null);
			col.setHeaderValue(annotationRecords_.get(i).getAnnotatorName());
			colModel.addColumn(col);
		}
		return colModel;
	}

	private void displayTagList() {
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
		tagTable_.setColumnModel(makeAnnotatorColumnModel());
		tagTable_.getTableHeader().repaint();

		TreeSet<String> availableTags = new TreeSet<String>(current_.getDatabase().getTags());
		availableTags.addAll(allTags); // new tags may not have been saved to the DB yet
		newTagModel_.removeAllElements();
		newTagModel_.addElement("");
		for(String tag : availableTags)
			newTagModel_.addElement(tag);
	}

	public boolean browseTo(String id, int target) {
		if(!id.equals("annot"))
			throw new IllegalArgumentException("Unexpected browse event from " + id);

		currentAnnotation_ = annotationRecords_.get(target);
		fireNavigationEvent();
		return true;
	}

	public void addNavigationListener(NavigationListener l) {
		navigationListeners_.add(l);
	}

	private void fireNavigationEvent() {
		for(NavigationListener l : navigationListeners_)
			l.navigate(current_, currentAnnotation_);
	}

	public void actionPerformed(ActionEvent e) {
		String[] cmd = e.getActionCommand().split(" ");
		if(cmd[0].equals("add-tag")) {
			String tag = (String) newTag_.getSelectedItem();
			if(tag.isEmpty())
				return;
			getResearcherAnnotationRecord().addTag(tag);
			newTag_.setSelectedItem("");
			displayTagList();
		} else if(cmd[0].equals("remove-tag")) {
			String tag = (String) newTag_.getSelectedItem();
			if(tag == null)
				return;
			getResearcherAnnotationRecord().removeTag(tag);
			newTag_.setSelectedItem("");
			displayTagList();
		}
	}

	private AnnotationRecord getResearcherAnnotationRecord() {
		for(AnnotationRecord rec : annotationRecords_)
			if(rec.getAnnotatorID() == annotator_id_)
				return rec;

		AnnotationRecord rec = current_.getAnnotationRecord(annotator_id_);
		annotationRecords_ = current_.getAnnotationRecords();
		return rec;
	}
}
