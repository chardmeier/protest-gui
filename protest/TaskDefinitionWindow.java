package protest;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

public class TaskDefinitionWindow implements ActionListener {
	private JFrame frame_;
	private JTable tgtcorpusTable_;
	private JTable categoryTable_;
	private JTextField labelField_;

	private SelectionTableModel tgtcorpusModel_;
	private SelectionTableModel categoryModel_;
	private SpinnerNumberModel taskSpinnerModel_;
	private SpinnerNumberModel iaaSpinnerModel_;

	public TaskDefinitionWindow(Database db) {
		frame_ = new JFrame("Task Definition");
		frame_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame_.getContentPane().setLayout(new GridLayout(1, 3));

		tgtcorpusModel_ = new SelectionTableModel();
		tgtcorpusTable_ = new JTable(tgtcorpusModel_);
		frame_.getContentPane().add(new JScrollPane(tgtcorpusTable_));

		categoryModel_ = new SelectionTableModel();
		List<AnnotationCategory> cats = db.getCategories();
		for(AnnotationCategory c : cats)
			categoryModel_.add(c.getLabel(), 0);

		categoryTable_ = new JTable(categoryModel_);
		frame_.getContentPane().add(new JScrollPane(categoryTable_));

		JPanel settingsPanel = new JPanel(new GridLayout(4, 2));
		frame_.getContentPane().add(settingsPanel);

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
		createButton.setActionListener(this);
		settingsPanel.add(createButton);

		JButton resetButton = new JButton("Reset");
		resetButton.setActionCommand("reset");
		resetButton.setActionListener(this);
		settingsPanel.add(resetButton);

		frame_.pack();
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();

		if(cmd.equals("create"))
			createTask();
		else if(cmd.equals("reset"))
			reset();
	}

	public void reset() {
		labelField_.setText("");
		taskSpinnerModel_.setValue(Integer.valueOf(1));
		iaaSpinnerModel_.setValue(Integer.valueOf(0));
	}

	public void setVisible(boolean visible) {
		frame_.setVisible(visible);
	}
}
