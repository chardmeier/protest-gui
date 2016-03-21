package protest;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.awt.FlowLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import protest.db.AnnotationCategory;
import protest.db.Database;
import protest.db.DatabaseException;
import protest.db.PrecheckReport;
import protest.db.TargetCorpus;
import protest.gui.fileselector.DatabaseOpener;
import protest.gui.instance.InstanceWindow;
import protest.gui.taskdef.TaskDefinitionWindow;

public class ProtestGUI implements Runnable, ActionListener, Observer {
	private Database db_;
	private List<AnnotationCategory> categories_;
	private int currentCategory_;

	private InstanceWindow instWindow_;
	private TaskDefinitionWindow taskDefinitionWindow_;

	private JFrame frame_;
	private ArrayList<JButton> catButtons_;

	public ProtestGUI() {
		String dbname = new DatabaseOpener().open();
		init(dbname);
	}

	public ProtestGUI(String dbfile) {
		init(dbfile);
	}

	private void init(String dbfile) {
		JDialog.setDefaultLookAndFeelDecorated(true);

		try {
			db_ = new Database(dbfile);
		} catch(DatabaseException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(),
					"Error opening file", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			System.exit(1);
		}
		instWindow_ = new InstanceWindow(getAnnotatorID());
		taskDefinitionWindow_ = new TaskDefinitionWindow(db_);
	}
   
	public int getAnnotatorID() {
		String id = db_.getMetadata("annotator_id");
		if(id.isEmpty())
			return -1;
		else
			return Integer.parseInt(id);
	}

	public void run() {
		frame_ = new JFrame(db_.getName());
		frame_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		((BorderLayout) frame_.getContentPane().getLayout()).setVgap(15);
		
		if(db_.getMetadata("file_type").equals("master")) {
			JToolBar toolbar = new JToolBar("Protest GUI");
			frame_.getContentPane().add(toolbar, BorderLayout.PAGE_START);

			JButton defineTaskButton = new JButton("Define Tasks");
			defineTaskButton.setActionCommand("define-tasks");
			defineTaskButton.addActionListener(this);
			toolbar.add(defineTaskButton);

			JButton assignButton = new JButton("Assign to Annotators");
			assignButton.setActionCommand("assign-annotators");
			assignButton.addActionListener(this);
			toolbar.add(assignButton);

			JButton createBatchButton = new JButton("Create Batch");
			createBatchButton.setActionCommand("create-batch");
			createBatchButton.addActionListener(this);
			toolbar.add(createBatchButton);

			JButton importBatchButton = new JButton("Import Batch");
			importBatchButton.setActionCommand("import-batch");
			importBatchButton.addActionListener(this);
			toolbar.add(importBatchButton);
		} else {
			String ann = String.format("Annotation batch for annotator %s", db_.getMetadata("annotator_name"));
			JLabel annLabel = new JLabel(ann, JLabel.CENTER);
			annLabel.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(5, 5, 5, 5),
						BorderFactory.createCompoundBorder(
							BorderFactory.createRaisedBevelBorder(),
							BorderFactory.createLoweredBevelBorder())));
			frame_.getContentPane().add(annLabel, BorderLayout.PAGE_START);
		}

		JPanel mainPanel = new JPanel(new BorderLayout());
		frame_.getContentPane().add(mainPanel, BorderLayout.CENTER);

		JLabel instructionLabel = new JLabel("<html><b>Select set of instances, for annotation / browsing:</b></html>");
		JPanel instructionPanel = new JPanel();
		instructionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		instructionPanel.add(instructionLabel);
		mainPanel.add(instructionPanel, BorderLayout.PAGE_START);
		
		categories_ = db_.getCategories();

		JPanel catButtonsPanel = new JPanel(new GridLayout(categories_.size() + 1, AnnotationCategory.GROUP_COUNT + 1));
		mainPanel.add(new JScrollPane(catButtonsPanel), BorderLayout.CENTER);
		
		catButtonsPanel.add(new JLabel());
		for(int i = 0; i < AnnotationCategory.GROUP_COUNT; i++)
			catButtonsPanel.add(new JLabel(AnnotationCategory.getGroupLabel(i), JLabel.CENTER));

		catButtons_ = new ArrayList<JButton>();
		for(int i = 0; i < categories_.size(); i++) {
			AnnotationCategory cat = categories_.get(i);
			catButtonsPanel.add(new JLabel(cat.getLabel()));
			for(int j = 0; j < AnnotationCategory.GROUP_COUNT; j++) {
				int cnt = cat.getCount(j);
				JButton button = new JButton(Integer.toString(cnt));
				button.setEnabled(cnt > 0);
				button.setBackground(AnnotationCategory.getGroupColour(j));
				button.setOpaque(true);
				button.setActionCommand(String.format("display %d %d", i, j));
				button.addActionListener(this);
				catButtons_.add(button);
				catButtonsPanel.add(button);
			}
		}

		JButton quitButton = new JButton("Quit");
		frame_.getContentPane().add(quitButton, BorderLayout.PAGE_END);
		quitButton.setActionCommand("quit");
		quitButton.addActionListener(this);

		frame_.setLocationByPlatform(true);
		frame_.pack();
		frame_.setVisible(true);

		// to refresh example counts on DB changes
		db_.addObserver(this);
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();

		if(cmd.equals("quit"))
			System.exit(0);
		else if(cmd.equals("define-tasks"))
			taskDefinitionWindow_.setVisible(true);
		else if(cmd.equals("assign-annotators"))
			System.err.println("assign-annotators not implemented");
		else if(cmd.equals("create-batch"))
			createAnnotationBatch();
		else if(cmd.equals("import-batch"))
			importAnnotationBatch();
		else if(cmd.startsWith("display")) {
			String[] idx = cmd.split(" ");
			currentCategory_ = Integer.parseInt(idx[1]);
			int grp = Integer.parseInt(idx[2]);

			instWindow_.setData(categories_.get(currentCategory_).getLabel(), categories_.get(currentCategory_).getExamples(grp));
			instWindow_.setVisible(true);
		}
	}

	private void createAnnotationBatch() {
		System.err.println("create-batch not implemented");
	}

	private void importAnnotationBatch() {
		try {
			// Get name of database to be imported
			String dbname = new DatabaseOpener().open();
			if(dbname == null)
				System.exit(0);
			// Get PrecheckReport object and test "canImport" (should be yes if compatible) and "shouldWarn"
			PrecheckReport rep = db_.precheckAnnotationBatch(dbname);
			// If PrecheckReport is ok, import annotations
			if (rep.canImport() == true) {
				if (rep.shouldWarn() == true) {
					int response = JOptionPane.showConfirmDialog(null, rep.getMessage(), "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					if (response != JOptionPane.YES_OPTION)
						return;
				}

				System.err.println("Importing...");
				System.err.println(dbname);
				db_.importAnnotationBatch(dbname);
			} else {
				String importMsg = "Unable to import annotator database: " + rep.getMessage();
				JOptionPane.showMessageDialog(null, importMsg);
			}
		} catch(DatabaseException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void update(Observable obs, Object o) {
		if(obs != db_)
			throw new IllegalArgumentException("Unexcepted observable triggered event.");

		categories_ = db_.getCategories();
		for(int i = 0; i < categories_.size(); i++) {
			for(int j = 0; j < AnnotationCategory.GROUP_COUNT; j++) {
				int cnt = categories_.get(i).getCount(j);
				JButton button = catButtons_.get(i * AnnotationCategory.GROUP_COUNT + j);
				button.setText(Integer.toString(cnt));
				button.setEnabled(cnt > 0);
			}
		}
	}

	public static void main(String[] args) {
		if(args.length > 1) {
			System.err.println("Usage: ProtestGUI [dbfile]");
			System.exit(1);
		}

		ProtestGUI gui;
		if(args.length == 1)
			gui = new ProtestGUI(args[0]);
		else
			gui = new ProtestGUI();

		SwingUtilities.invokeLater(gui);
	}
}
