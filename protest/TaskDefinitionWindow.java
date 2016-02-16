package protest;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class TaskDefinitionWindow implements ActionListener, ListSelectionListener {
	private Database db_;

	private JFrame frame_;
	private JLabel countLabel_;
	private JTable tgtcorpusTable_;
	private JTable categoryTable_;
	private JTextField labelField_;

	private SelectionTableModel tgtcorpusModel_;
	private SelectionTableModel categoryModel_;
	private SpinnerNumberModel taskSpinnerModel_;
	private SpinnerNumberModel iaaSpinnerModel_;

	public TaskDefinitionWindow(Database db) {
		db_ = db;

		frame_ = new JFrame("Task Definition");
		frame_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		tgtcorpusModel_ = new SelectionTableModel();
		List<TargetCorpus> corpora = db.getTargetCorpora();
		for(TargetCorpus c : corpora)
			tgtcorpusModel_.add(c.getID(), c.getLabel(), c.getCount());

		tgtcorpusTable_ = new JTable(tgtcorpusModel_);
		tgtcorpusTable_.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tgtcorpusTable_.setRowSelectionAllowed(true);
		tgtcorpusTable_.setColumnSelectionAllowed(false);
		tgtcorpusTable_.getSelectionModel().addListSelectionListener(this);

		JPanel tgtcorpusPanel = new JPanel();
		tgtcorpusPanel.add(new JScrollPane(tgtcorpusTable_));

		categoryModel_ = new SelectionTableModel();
		List<AnnotationCategory> cats = db.getCategories();
		for(AnnotationCategory c : cats)
			categoryModel_.add(c.getID(), c.getLabel(), 0);

		categoryTable_ = new JTable(categoryModel_);
		categoryTable_.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		categoryTable_.setRowSelectionAllowed(true);
		categoryTable_.setColumnSelectionAllowed(false);
		categoryTable_.getSelectionModel().addListSelectionListener(this);
		
		JPanel categoryPanel = new JPanel();
		categoryPanel.add(new JScrollPane(categoryTable_));

		JPanel settingsPanel = new JPanel(new GridLayout(5, 2));

		settingsPanel.add(new JLabel("Number of examples:"));
		countLabel_ = new JLabel("0");
		settingsPanel.add(countLabel_);

		settingsPanel.add(new JLabel("Task label:"));
		labelField_ = new JTextField(20);
		settingsPanel.add(labelField_);

		settingsPanel.add(new JLabel("Number of tasks:"));
		taskSpinnerModel_ = new SpinnerNumberModel(1, 1, 1, 1);
		settingsPanel.add(new JSpinner(taskSpinnerModel_));

		settingsPanel.add(new JLabel("IAA set size:"));
		iaaSpinnerModel_ = new SpinnerNumberModel(0, 0, 0, 1);
		settingsPanel.add(new JSpinner(iaaSpinnerModel_));

		JButton createButton = new JButton("Create task");
		createButton.setActionCommand("create");
		createButton.addActionListener(this);
		settingsPanel.add(createButton);

		JButton resetButton = new JButton("Reset");
		resetButton.setActionCommand("reset");
		resetButton.addActionListener(this);
		settingsPanel.add(resetButton);

		GroupLayout layout = new GroupLayout(frame_.getContentPane());
		frame_.getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addGroup(layout.createSequentialGroup()
						.addComponent(tgtcorpusPanel)
						.addComponent(categoryPanel))
					.addComponent(settingsPanel));
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup()
						.addComponent(tgtcorpusPanel)
						.addComponent(categoryPanel))
					.addComponent(settingsPanel));
		layout.linkSize(SwingConstants.HORIZONTAL, tgtcorpusPanel, categoryPanel);
		layout.linkSize(SwingConstants.HORIZONTAL, tgtcorpusPanel, settingsPanel);

		frame_.pack();
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();

		if(cmd.equals("create"))
			createTasks();
		else if(cmd.equals("reset"))
			reset();
	}

	public void valueChanged(ListSelectionEvent e) {
		if(e.getValueIsAdjusting())
			return;
		if(e.getSource() == tgtcorpusTable_.getSelectionModel())
			updateCategoryCounts();
		updateTaskParameters();
	}

	private void updateCategoryCounts() {
		int[] tgtcorpora = tgtcorpusModel_.translateSelection(tgtcorpusTable_.getSelectedRows());
		List<AnnotationCategory> filteredCats = db_.getCategoriesForCorpora(tgtcorpora);
		categoryModel_.resetCounts();
		for(AnnotationCategory cat : filteredCats)
			categoryModel_.setCount(cat.getLabel(), cat.getCount());
	}

	private void updateTaskParameters() {
		int[] tgtcorpora = tgtcorpusModel_.translateSelection(tgtcorpusTable_.getSelectedRows());
		int[] categories = categoryModel_.translateSelection(categoryTable_.getSelectedRows());
		int cnt = db_.getFilteredExampleCount(tgtcorpora, categories);
		countLabel_.setText(Integer.toString(cnt));
		if(taskSpinnerModel_.getNumber().intValue() > cnt)
			taskSpinnerModel_.setValue(Integer.valueOf(cnt));
		taskSpinnerModel_.setMaximum(Integer.valueOf(cnt));
		if(iaaSpinnerModel_.getNumber().intValue() > cnt)
			iaaSpinnerModel_.setValue(Integer.valueOf(cnt));
		iaaSpinnerModel_.setMaximum(Integer.valueOf(cnt));
	}

	private void createTasks() {
		String label = labelField_.getText();
		int[] tgtcorpora = tgtcorpusTable_.getSelectedRows();
		int[] categories = categoryTable_.getSelectedRows();
		int ntasks = taskSpinnerModel_.getNumber().intValue();
		int iaa = iaaSpinnerModel_.getNumber().intValue();
		int cnt = db_.getFilteredExampleCount(tgtcorpora, categories);

		if(cnt == 0) {
			JOptionPane.showMessageDialog(null, "No examples selected",
					"No examples", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if(ntasks + iaa == 0) {
			JOptionPane.showMessageDialog(null, "No tasks specified",
					"No tasks", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if(ntasks + iaa > cnt) {
			JOptionPane.showMessageDialog(null, "Not enough examples in selection to create the specified tasks.",
					"Too few examples", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if(db_.tasksetExists(label) &&
			JOptionPane.showConfirmDialog(null, String.format("Task set %s already exists. Replace it?", label),
					"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
				return;

		db_.createAnnotationTasks(label, tgtcorpora, categories, ntasks, iaa);
	}

	private void reset() {
		labelField_.setText("");
		taskSpinnerModel_.setValue(Integer.valueOf(1));
		iaaSpinnerModel_.setValue(Integer.valueOf(0));
	}

	public void setVisible(boolean visible) {
		frame_.setVisible(visible);
	}
}
